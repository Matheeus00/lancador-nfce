package com.fritz.lancador.enums;

public enum Item {
    CAFE_MANHA("Café da Manhã"),
    ALMOCO("Almoço"),
    CAFE_TARDE("Café da Tarde"),
    LANCHE("Lanche"),
    AGUA("Água"),
    REFRIGERANTE("Refrigerante");

    private String label;

    Item(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
