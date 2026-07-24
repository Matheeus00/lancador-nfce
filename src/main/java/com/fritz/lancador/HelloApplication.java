package com.fritz.lancador;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Group;
import com.dlsc.formsfx.model.structure.NodeElement;
import com.dlsc.formsfx.view.controls.SimpleRadioButtonControl;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.fritz.lancador.components.SearchField;
import com.fritz.lancador.enums.Item;
import com.fritz.lancador.model.DataForm;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public class HelloApplication extends Application {
    private final StringProperty searchCostumerProperty = new SimpleStringProperty("");

    @Override
    public void start(Stage stage) {
        DataForm dataForm = new DataForm();

        ListProperty<Item> itemsObservable = new SimpleListProperty<>(
            FXCollections.observableArrayList(Item.values())
        );

        Label labelNota = new Label("Dados nota");
        labelNota.getStyleClass().add("form-section-title");

        Label labelCostumer = new Label("Cliente");
        labelCostumer.getStyleClass().add("form-section-title");

        Form form = Form.of(
            Group.of(
                NodeElement.of(labelNota),
                Field.ofDoubleType(dataForm.valueProperty())
                     .label("Valor (R$)"),
                Field.ofSingleSelectionType(itemsObservable, dataForm.itemProperty())
                    .styleClass("radio-button")
                    .render(SimpleRadioButtonControl::new)
                    .label("Item")
            ),
            Group.of(
                NodeElement.of(labelCostumer),
                Field.ofStringType(searchCostumerProperty)
                     .label("Pesquisa")
                     .placeholder("Nome/CPF/CNPJ")
                     .render(new SearchField()),
                Field.ofMultiSelectionType(Arrays.asList("Zürich (ZH)", "Bern (BE)"), Collections.singletonList(0))
            )
        ).title("Formulário");

        FormRenderer rendered = new FormRenderer(form);

        StackPane root = new StackPane();

        String cssPath = Objects.requireNonNull(getClass().getResource("/css/windows-theme.css")).toExternalForm();

        Button btnEmit = new Button("Emitir");
        btnEmit.getStyleClass().add("button-primary");
        btnEmit.setOnAction(e -> {
            form.persist();

            if (form.isValid()) {
                System.out.println("Objeto salvo com sucesso!");
            } else {
                System.out.println("Formulário contém erros. Corrija-os antes de salvar.");
            }
        });

        Button btnClean = new Button("Limpar");
        btnEmit.getStyleClass().add("button");
        btnClean.setOnAction(e -> {
            ButtonType bntYes = new ButtonType("Sim");
            ButtonType btnNo = new ButtonType("Não");

            Alert alertDialog = new Alert(Alert.AlertType.CONFIRMATION);
            alertDialog.getDialogPane().getStylesheets().add(cssPath);
            alertDialog.setTitle("Limpar tudo");
            alertDialog.setHeaderText("Confirmação");
            alertDialog.setContentText("Deseja realmente limpar o formulário?");
            alertDialog.getButtonTypes().setAll(bntYes, btnNo);

            Optional<ButtonType> result = alertDialog.showAndWait();

            if (result.isPresent() && result.get() == bntYes) {
                form.reset();
            }
        });

        HBox btnPanel = new HBox(15);
        btnPanel.setAlignment(javafx.geometry.Pos.CENTER);
        btnPanel.setStyle("-fx-padding: 15 0 0 0;");
        btnPanel.getChildren().addAll(btnEmit, btnClean);

        VBox vbox = new VBox(rendered, btnPanel);
        vbox.getStyleClass().add("panel-container");
        root.getChildren().add(vbox);

        Scene scene = new Scene(root, 800, 800);
        scene.getStylesheets().add(cssPath);

        stage.setTitle("Lançador");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        Platform.runLater(stage::centerOnScreen);
    }
}
