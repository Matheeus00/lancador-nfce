package com.fritz.lancador.model;

import com.fritz.lancador.enums.Item;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

public class DataForm {
    private final DoubleProperty value = new SimpleDoubleProperty();
    private final ObjectProperty<Item> item = new SimpleObjectProperty<>(Item.ALMOCO);

    public DoubleProperty valueProperty() { return value; }

    public ObjectProperty<Item> itemProperty() { return item; }

}
