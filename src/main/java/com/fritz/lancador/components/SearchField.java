package com.fritz.lancador.components;

import com.dlsc.formsfx.view.controls.SimpleTextControl;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class SearchField extends SimpleTextControl {

    private HBox container;
    private TextField textField;
    private Label fieldLabel;
    private Button button;

    @Override
    public void initializeParts() {
        super.initializeParts();
        container = new HBox(8);

        textField = new TextField();
        button = new Button("Buscar");
        button.setOnAction(e -> {
            System.out.println("Botão clicado! Valor atual: " + field.getValue());
        });
        fieldLabel = new Label();

        textField.textProperty().bindBidirectional(field.valueProperty());
    }

    @Override
    public void layoutParts() {
        container.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(textField, Priority.ALWAYS);
        textField.setPrefWidth(470);

        container.getChildren().addAll(fieldLabel, textField, button);

        javafx.scene.layout.GridPane.setMargin(fieldLabel, new javafx.geometry.Insets(0, 56, 0, 0));

        int colSpan = field.getSpan();
        add(fieldLabel, 0, 0, 1, 1);
        add(container, 1, 0, colSpan - 1, 1);
    }

    @Override
    public void setupBindings() {
        super.setupBindings();

        textField.promptTextProperty().bind(field.placeholderProperty());
        fieldLabel.textProperty().bind(field.labelProperty());
    }
}
