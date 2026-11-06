package com.fritz.lancador.models;

import java.util.Objects;

public class Costumer {
    private final String name;
    private final String cpf;

    public Costumer(String name, String cpf) {
        this.name = name == null ? "" : name.trim();
        this.cpf = Objects.requireNonNull(cpf, "cpf");
    }

    public String getName() {
        return name;
    }

    public String getCpf() {
        return cpf;
    }

    @Override
    public String toString() {
        return name.isEmpty() ? "CPF " + cpf : name + " — CPF " + cpf;
    }
}
