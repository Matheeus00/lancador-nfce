package com.fritz.lancador;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.structure.NodeElement;
import com.dlsc.formsfx.view.controls.SimpleRadioButtonControl;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.fritz.lancador.components.CpfTextFormatter;
import com.fritz.lancador.components.SearchField;
import com.fritz.lancador.enums.Item;
import com.fritz.lancador.models.Costumer;
import com.fritz.lancador.models.DataForm;
import com.fritz.lancador.services.CertificateSettings;
import com.fritz.lancador.services.CostumerService;
import com.fritz.lancador.services.SefazService;
import com.fritz.lancador.services.ThermalPrinterService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.BackingStoreException;

public class HelloApplication extends Application {
    private final StringProperty searchCostumerProperty = new SimpleStringProperty("");

    private final DataForm dataForm = new DataForm();

    private final CostumerService costumerService = new CostumerService();
    private final CertificateSettings certificateSettings = new CertificateSettings();
    private final SefazService sefazService = new SefazService();
    private final ThermalPrinterService thermalPrinterService = new ThermalPrinterService();

    private final String cssPath = Objects.requireNonNull(getClass().getResource("/css/windows-theme.css")).toExternalForm();

    private Form form;
    private Stage primaryStage;
    private Label certificateStatus;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        buildForm();

        initApplication(stage);
    }

    private void buildForm() {
        Label labelNota = new Label("Dados nota");
        labelNota.getStyleClass().add("form-section-title");

        Label labelCostumer = new Label("Cliente");
        labelCostumer.getStyleClass().add("form-section-title");

        SearchField customerSearchField = new SearchField();
        customerSearchField.setOnSearch(this::openCostumerDialog);

        this.form = Form.of(
            Group.of(
                NodeElement.of(labelNota),
                Field.ofDoubleType(dataForm.valueProperty())
                     .label("Valor (R$)"),
                Field.ofSingleSelectionType(new SimpleListProperty<>(FXCollections.observableArrayList(Item.values())), dataForm.itemProperty())
                    .styleClass("radio-button")
                    .render(SimpleRadioButtonControl::new)
                    .label("Item")
            ),
            Group.of(
                NodeElement.of(labelCostumer),
                Field.ofStringType(searchCostumerProperty)
                     .label("Pesquisa")
                     .placeholder("Pesquise por nome ou CPF")
                     .render(customerSearchField)
            )
        ).title("Lançamento");
    }

    private void initApplication(Stage stage) {
        FormRenderer rendered = new FormRenderer(form);

        StackPane root = new StackPane();

        Label pageTitle = new Label("Lançador NFC-e");
        pageTitle.getStyleClass().add("page-title");

        Label pageSubtitle = new Label("Preencha os dados e imprima uma prévia do atendimento.");
        pageSubtitle.getStyleClass().add("page-subtitle");

        Button btnCustomers = new Button("Clientes");
        btnCustomers.setOnAction(e -> openCostumerDialog(""));

        Button btnCertificate = new Button("Certificado A1");
        btnCertificate.setOnAction(e -> chooseCertificate());

        HBox headerActions = new HBox(10, btnCustomers, btnCertificate);
        headerActions.setAlignment(Pos.CENTER_RIGHT);

        VBox titleBlock = new VBox(4, pageTitle, pageSubtitle);
        HBox pageHeader = new HBox(20, titleBlock, headerActions);
        pageHeader.getStyleClass().add("page-header");
        pageHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        certificateStatus = new Label();
        certificateStatus.getStyleClass().add("certificate-status");
        updateCertificateStatus();

        HBox btnPanel = new HBox(12);
        btnPanel.setAlignment(Pos.CENTER_RIGHT);
        btnPanel.getStyleClass().add("actions-bar");

        buildButtons(btnPanel, stage);

        VBox vbox = new VBox(18, pageHeader, certificateStatus, rendered, btnPanel);
        vbox.getStyleClass().add("panel-container");
        vbox.setMaxWidth(1120);
        VBox.setVgrow(rendered, Priority.ALWAYS);
        root.getChildren().add(vbox);

        Scene scene = new Scene(root, 860, 800);
        scene.getStylesheets().add(cssPath);

        stage.setTitle("Lançador");
        stage.setScene(scene);
        stage.setMinWidth(700);
        stage.setMinHeight(620);
        stage.setResizable(true);
        stage.fullScreenProperty().addListener((observable, wasFullScreen, isFullScreen) -> {
            if (isFullScreen) {
                root.getStyleClass().add("fullscreen-mode");
            } else {
                root.getStyleClass().remove("fullscreen-mode");
            }
        });
        stage.show();

        Platform.runLater(stage::centerOnScreen);
    }

    private void buildButtons(Pane component, Stage stage) {
        Button btnEmit = new Button("Emitir");
        btnEmit.getStyleClass().add("button-primary");
        btnEmit.setOnAction(e -> {
            form.persist();

            if (form.isValid()) {
                showAlert(Alert.AlertType.WARNING, "Emissão não disponível",
                    sefazService.isConfigured()
                        ? "A integração fiscal ainda não implementa a autorização da NFC-e."
                        : "Integração com provedor fiscal não configurada. Nenhuma NFC-e foi emitida.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Dados inválidos",
                    "Formulário contém erros. Corrija-os antes de emitir.");
            }
        });

        Button btnPrint = new Button("Imprimir prévia");
        btnPrint.getStyleClass().add("button-secondary");
        btnPrint.setOnAction(e -> {
            form.persist();

            if (!form.isValid()) {
                showAlert(Alert.AlertType.ERROR, "Dados inválidos",
                    "Formulário contém erros. Corrija-os antes de imprimir.");
                return;
            }

            ThermalPrinterService.PrintResult result = thermalPrinterService.printDraft(
                dataForm, searchCostumerProperty.get(), btnPrint.getScene().getWindow());
            switch (result) {
                case PRINTED:
                    showAlert(Alert.AlertType.INFORMATION, "Prévia impressa",
                        "A prévia não fiscal foi enviada para a impressora selecionada.");
                    break;
                case NO_PRINTER:
                    showAlert(Alert.AlertType.ERROR, "Impressora indisponível",
                        "Nenhuma impressora foi encontrada. Instale e configure a impressora térmica no sistema.");
                    break;
                case FAILED:
                    showAlert(Alert.AlertType.ERROR, "Falha na impressão",
                        "Não foi possível enviar a prévia para a impressora.");
                    break;
                case CANCELLED:
                    break;
                default:
                    throw new IllegalStateException("Resultado de impressão desconhecido: " + result);
            }
        });

        Button btnClean = new Button("Limpar");
        btnClean.getStyleClass().add("button");
        btnClean.setOnAction(e -> {
            ButtonType bntYes = new ButtonType("Sim");
            ButtonType btnNo = new ButtonType("Não");

            Alert alertDialog = new Alert(Alert.AlertType.CONFIRMATION, "Deseja realmente limpar o formulário?", bntYes, btnNo);
            alertDialog.getDialogPane().getStylesheets().add(cssPath);

            Optional<ButtonType> result = alertDialog.showAndWait();

            if (result.isPresent() && result.get() == bntYes) {
                form.reset();
                dataForm.valueProperty().set(0);
                dataForm.itemProperty().set(Item.ALMOCO);
                searchCostumerProperty.set("");
            }
        });

        Button btnFullscreen = new Button("Tela cheia");
        btnFullscreen.getStyleClass().add("button-secondary");
        btnFullscreen.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        stage.fullScreenProperty().addListener((observable, wasFullScreen, isFullScreen) ->
            btnFullscreen.setText(isFullScreen ? "Sair da tela cheia" : "Tela cheia"));

        component.getChildren().addAll(btnFullscreen, btnClean, btnPrint, btnEmit);
    }

    private void chooseCertificate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar e validar certificado digital A1");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Certificados A1 (*.pfx, *.p12)", "*.pfx", "*.p12"));

        String currentPath = certificateSettings.getCertificatePath();
        if (!currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.getParentFile() != null && currentFile.getParentFile().isDirectory()) {
                fileChooser.setInitialDirectory(currentFile.getParentFile());
            }
        }

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile == null) {
            return;
        }

        validateAndSaveCertificate(selectedFile);
    }

    private void validateAndSaveCertificate(File certificateFile) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Validar certificado A1");
        dialog.setHeaderText("Informe a senha do certificado. Ela será usada somente para validar o arquivo.");
        dialog.initOwner(primaryStage);
        dialog.getDialogPane().getStylesheets().add(cssPath);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Senha do certificado");
        VBox content = new VBox(10, new Label(certificateFile.getName()), passwordField);
        content.setPrefWidth(400);
        dialog.getDialogPane().setContent(content);

        ButtonType validateButton = new ButtonType("Validar e salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(validateButton, ButtonType.CANCEL);
        SefazService.CertificateInfo[] certificateInfo = new SefazService.CertificateInfo[1];

        Button validateDialogButton = (Button) dialog.getDialogPane().lookupButton(validateButton);
        validateDialogButton.addEventFilter(ActionEvent.ACTION, event -> {
            char[] password = passwordField.getText().toCharArray();
            try {
                SefazService.CertificateInfo validatedCertificate = sefazService.validateCertificate(
                    Paths.get(certificateFile.getAbsolutePath()), password);
                certificateSettings.saveCertificatePath(certificateFile.getAbsolutePath());
                certificateInfo[0] = validatedCertificate;
                updateCertificateStatus();
                passwordField.clear();
            } catch (IOException | java.security.GeneralSecurityException ex) {
                event.consume();
                showDialogAlert(dialog, Alert.AlertType.ERROR, "Falha na validação",
                    "Não foi possível validar o certificado. Verifique o arquivo e a senha.");
            } catch (BackingStoreException ex) {
                event.consume();
                showDialogAlert(dialog, Alert.AlertType.ERROR, "Erro ao salvar certificado",
                    "O certificado foi validado, mas não foi possível salvar o caminho: " + ex.getMessage());
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        });
        dialog.setOnShown(event -> passwordField.requestFocus());
        dialog.showAndWait();

        if (certificateInfo[0] != null) {
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            showAlert(Alert.AlertType.INFORMATION, "Certificado validado",
                "Certificado A1 válido e salvo.\nTitular: " + certificateInfo[0].getSubject()
                    + "\nVálido até: " + certificateInfo[0].getExpirationDate().toInstant()
                    .atZone(ZoneId.systemDefault()).format(dateFormat)
                    + "\nA senha não foi armazenada. A emissão fiscal ainda não está configurada.");
        }
    }

    private void updateCertificateStatus() {
        String path = certificateSettings.getCertificatePath();
        String filename = path.isEmpty() ? "" : new File(path).getName();
        certificateStatus.setText(path.isEmpty()
            ? "Certificado A1: não configurado"
            : "Certificado A1: " + filename + " (somente caminho salvo)");
    }

    private void openCostumerDialog(String initialQuery) {
        Dialog<Costumer> dialog = new Dialog<>();
        dialog.setTitle("Clientes");
        dialog.setHeaderText("Pesquise por nome ou CPF, selecione um cliente ou cadastre outro.");
        dialog.initOwner(primaryStage);
        dialog.getDialogPane().getStylesheets().add(cssPath);

        TextField searchField = new TextField(initialQuery == null ? "" : initialQuery);
        searchField.setPromptText("Nome ou CPF");
        ListView<Costumer> customersList = new ListView<>();
        customersList.setPrefHeight(260);
        customersList.setPlaceholder(new Label("Nenhum cliente encontrado."));

        Runnable refreshCustomers = () -> {
            try {
                customersList.getItems().setAll(costumerService.search(searchField.getText()));
            } catch (IOException ex) {
                customersList.getItems().clear();
                showAlert(Alert.AlertType.ERROR, "Erro ao carregar clientes",
                    "Não foi possível ler os cadastros locais: " + ex.getMessage());
            }
        };
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshCustomers.run());
        refreshCustomers.run();

        Button addButton = new Button("Cadastrar cliente");
        addButton.getStyleClass().add("button-secondary");
        addButton.setOnAction(e -> {
            addCostumer();
            refreshCustomers.run();
        });

        Button editButton = new Button("Editar selecionado");
        editButton.getStyleClass().add("button-secondary");
        editButton.setOnAction(e -> {
            Costumer selected = customersList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showDialogAlert(dialog, Alert.AlertType.INFORMATION, "Selecione um cliente",
                    "Escolha um cliente da lista para editar.");
                return;
            }
            editCostumer(selected);
            refreshCustomers.run();
        });

        Button deleteButton = new Button("Excluir selecionado");
        deleteButton.setOnAction(e -> {
            Costumer selected = customersList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir o cadastro de " + selected + "?", ButtonType.YES, ButtonType.NO);
            confirmation.initOwner(dialog.getDialogPane().getScene().getWindow());
            confirmation.getDialogPane().getStylesheets().add(cssPath);
            Optional<ButtonType> answer = confirmation.showAndWait();
            if (answer.isPresent() && answer.get() == ButtonType.YES) {
                try {
                    costumerService.delete(selected);
                    refreshCustomers.run();
                } catch (IOException ex) {
                    showAlert(Alert.AlertType.ERROR, "Erro ao excluir cliente",
                        "Não foi possível atualizar os cadastros locais: " + ex.getMessage());
                }
            }
        });

        HBox customerActions = new HBox(10, addButton, editButton, deleteButton);
        customerActions.setAlignment(Pos.CENTER_RIGHT);
        VBox content = new VBox(12, searchField, customersList, customerActions);
        content.setPrefWidth(520);
        dialog.getDialogPane().setContent(content);

        ButtonType selectButton = new ButtonType("Selecionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButton, ButtonType.CLOSE);
        dialog.setResultConverter(button -> button == selectButton
            ? customersList.getSelectionModel().getSelectedItem()
            : null);
        dialog.setOnShown(event -> searchField.requestFocus());

        Optional<Costumer> result = dialog.showAndWait();
        result.ifPresent(this::selectCostumer);
    }

    private void addCostumer() {
        Dialog<Costumer> dialog = new Dialog<>();
        dialog.setTitle("Cadastrar cliente");
        dialog.setHeaderText("Os dados ficam salvos localmente neste computador.");
        dialog.initOwner(primaryStage);
        dialog.getDialogPane().getStylesheets().add(cssPath);

        TextField nameField = new TextField();
        nameField.setPromptText("Nome (opcional)");
        TextField cpfField = new TextField();
        cpfField.setPromptText("000.000.000-00");
        cpfField.setTextFormatter(CpfTextFormatter.create());

        VBox content = new VBox(10,
            new Label("Nome (opcional)"), nameField,
            new Label("CPF"), cpfField);
        content.setPrefWidth(420);
        dialog.getDialogPane().setContent(content);

        ButtonType saveButton = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        Button saveDialogButton = (Button) dialog.getDialogPane().lookupButton(saveButton);
        saveDialogButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                costumerService.save(nameField.getText(), cpfField.getText());
            } catch (IllegalArgumentException ex) {
                event.consume();
                showDialogAlert(dialog, Alert.AlertType.WARNING, "Dados inválidos", ex.getMessage());
            } catch (IOException ex) {
                event.consume();
                showDialogAlert(dialog, Alert.AlertType.ERROR, "Erro ao salvar cliente",
                    "Não foi possível salvar os dados no SQLite: " + ex.getMessage());
            }
        });
        dialog.setOnShown(event -> nameField.requestFocus());
        dialog.showAndWait();
    }

    private void editCostumer(Costumer customer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Editar cliente");
        dialog.setHeaderText("Altere o nome ou CPF do cliente.");
        dialog.initOwner(primaryStage);
        dialog.getDialogPane().getStylesheets().add(cssPath);

        TextField nameField = new TextField(customer.getName());
        nameField.setPromptText("Nome (opcional)");
        TextField cpfField = new TextField(customer.getCpf());
        cpfField.setPromptText("000.000.000-00");
        cpfField.setTextFormatter(CpfTextFormatter.create());

        VBox content = new VBox(10,
            new Label("Nome (opcional)"), nameField,
            new Label("CPF"), cpfField);
        content.setPrefWidth(420);
        dialog.getDialogPane().setContent(content);

        ButtonType saveButton = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);
        Button saveDialogButton = (Button) dialog.getDialogPane().lookupButton(saveButton);
        saveDialogButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                costumerService.update(customer, nameField.getText(), cpfField.getText());
            } catch (IllegalArgumentException ex) {
                event.consume();
                showDialogAlert(dialog, Alert.AlertType.WARNING, "Dados inválidos", ex.getMessage());
            } catch (IOException ex) {
                event.consume();
                showDialogAlert(dialog, Alert.AlertType.ERROR, "Erro ao editar cliente",
                    "Não foi possível atualizar os dados no SQLite: " + ex.getMessage());
            }
        });
        dialog.setOnShown(event -> nameField.requestFocus());
        dialog.showAndWait();
    }

    private void showDialogAlert(Dialog<?> owner, Alert.AlertType type, String title, String message) {
        Alert alertDialog = new Alert(type, message);
        alertDialog.setTitle(title);
        alertDialog.initOwner(owner.getDialogPane().getScene().getWindow());
        alertDialog.getDialogPane().getStylesheets().add(cssPath);
        alertDialog.showAndWait();
    }

    private void selectCostumer(Costumer customer) {
        searchCostumerProperty.set(customer.getName().isEmpty()
            ? "CPF " + CostumerService.formatCpf(customer.getCpf())
            : customer.getName() + " (CPF " + CostumerService.formatCpf(customer.getCpf()) + ")");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alertDialog = new Alert(type, message);
        alertDialog.setTitle(title);
        alertDialog.getDialogPane().getStylesheets().add(cssPath);
        alertDialog.show();
    }
}
