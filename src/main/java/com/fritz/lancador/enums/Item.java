package com.fritz.lancador.enums;

public enum Item {
    CAFE_MANHA("Café da Manhã"),
    ALMOCO("Almoço"),
    CAFE_TARDE("Café da Tarde");

    private String label;

    Item(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
