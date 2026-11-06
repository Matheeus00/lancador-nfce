package com.fritz.lancador.services;

import com.fritz.lancador.models.Costumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CostumerService {
    private final Path databaseFile;

    public CostumerService() {
        this(Paths.get(System.getProperty("user.home"), ".lancador", "lancador.db"));
    }

    CostumerService(Path databaseFile) {
        this.databaseFile = databaseFile.toAbsolutePath();
    }

    public List<Costumer> list() throws IOException {
        List<Costumer> customers = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT name, cpf FROM customers");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                 customers.add(new Costumer(resultSet.getString("name"),
                     formatCpf(resultSet.getString("cpf"))));
            }
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
        customers.sort(Comparator.comparing(customer -> normalizeName(customer.getName())));
        return customers;
    }

    public List<Costumer> search(String query) throws IOException {
        String normalizedName = normalizeName(query == null ? "" : query);
        String normalizedCpf = digitsOnly(query == null ? "" : query);
        List<Costumer> matches = new ArrayList<>();
        for (Costumer customer : list()) {
            if (normalizedName.isEmpty()
                || normalizeName(customer.getName()).contains(normalizedName)
                || (!normalizedCpf.isEmpty() && digitsOnly(customer.getCpf()).contains(normalizedCpf))) {
                matches.add(customer);
            }
        }
        return matches;
    }

    public void save(String name, String cpf) throws IOException {
        String trimmedName = name == null ? "" : name.trim();
        String normalizedCpf = digitsOnly(cpf == null ? "" : cpf);
        if (!isValidCpf(normalizedCpf)) {
            throw new IllegalArgumentException("Informe um CPF válido com 11 dígitos.");
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO customers (name, cpf) VALUES (?, ?)")) {
            statement.setString(1, trimmedName);
            statement.setString(2, normalizedCpf);
            statement.executeUpdate();
        } catch (SQLException ex) {
            if (isUniqueConstraint(ex)) {
                throw new IllegalArgumentException("Já existe um cliente cadastrado com este CPF.");
            }
            throw databaseError(ex);
        }
    }

    public void update(Costumer original, String name, String cpf) throws IOException {
        String trimmedName = name == null ? "" : name.trim();
        String normalizedCpf = digitsOnly(cpf == null ? "" : cpf);
        if (!isValidCpf(normalizedCpf)) {
            throw new IllegalArgumentException("Informe um CPF válido com 11 dígitos.");
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE customers SET name = ?, cpf = ? WHERE cpf = ?")) {
            statement.setString(1, trimmedName);
            statement.setString(2, normalizedCpf);
            statement.setString(3, digitsOnly(original.getCpf()));
            if (statement.executeUpdate() == 0) {
                throw new IOException("O cliente não foi encontrado para atualização.");
            }
        } catch (SQLException ex) {
            if (isUniqueConstraint(ex)) {
                throw new IllegalArgumentException("Já existe um cliente cadastrado com este CPF.");
            }
            throw databaseError(ex);
        }
    }

    public void delete(Costumer customer) throws IOException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM customers WHERE cpf = ?")) {
            statement.setString(1, digitsOnly(customer.getCpf()));
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw databaseError(ex);
        }
    }

    public static String formatCpf(String cpf) {
        String digits = digitsOnly(cpf);
        if (digits.length() != 11) {
            return cpf;
        }
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "."
            + digits.substring(6, 9) + "-" + digits.substring(9);
    }

    private Connection openConnection() throws SQLException, IOException {
        Path parent = databaseFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        try {
            initializeSchema(connection);
            return connection;
        } catch (SQLException ex) {
            connection.close();
            throw ex;
        }
    }

    private static void initializeSchema(Connection connection) throws SQLException {
        int schemaVersion;
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA user_version");
             ResultSet resultSet = statement.executeQuery()) {
            schemaVersion = resultSet.next() ? resultSet.getInt(1) : 0;
        }
        if (schemaVersion >= 1) {
            return;
        }

        connection.setAutoCommit(false);
        try {
            boolean tableExists;
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'customers'");
                 ResultSet resultSet = statement.executeQuery()) {
                tableExists = resultSet.next();
            }

            if (tableExists) {
                try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE customers_new ("
                        + "cpf TEXT PRIMARY KEY NOT NULL, "
                        + "name TEXT NOT NULL DEFAULT '')")) {
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO customers_new (cpf, name) SELECT cpf, name FROM customers")) {
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                    "DROP TABLE customers")) {
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                    "ALTER TABLE customers_new RENAME TO customers")) {
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE customers ("
                        + "cpf TEXT PRIMARY KEY NOT NULL, "
                        + "name TEXT NOT NULL DEFAULT '')")) {
                    statement.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement("PRAGMA user_version = 1")) {
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static boolean isUniqueConstraint(SQLException exception) {
        return exception.getErrorCode() == 19;
    }

    private static IOException databaseError(SQLException exception) {
        return new IOException("Não foi possível acessar o banco de clientes SQLite.", exception);
    }

    private static boolean isValidCpf(String cpf) {
        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        int firstDigit = cpfCheckDigit(cpf, 9);
        int secondDigit = cpfCheckDigit(cpf, 10);
        return cpf.charAt(9) - '0' == firstDigit && cpf.charAt(10) - '0' == secondDigit;
    }

    private static int cpfCheckDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (cpf.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    private static String digitsOnly(String value) {
        return value.replaceAll("\\D", "");
    }

    private static String normalizeName(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .trim();
    }
}
