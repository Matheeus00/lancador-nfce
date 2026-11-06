package com.fritz.lancador.services;

import com.fritz.lancador.models.Costumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CostumerServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesCustomersAndSearchesByNameOrCpfAcrossInstances() throws IOException {
        CostumerService service = new CostumerService(temporaryDirectory.resolve("clients.db"));
        service.save("João da Silva", "529.982.247-25");

        CostumerService reopenedService = new CostumerService(temporaryDirectory.resolve("clients.db"));
        List<Costumer> byName = reopenedService.search("joao");
        List<Costumer> byCpf = reopenedService.search("982247");

        assertEquals(1, byName.size());
        assertEquals("João da Silva", byName.get(0).getName());
        assertEquals(1, byCpf.size());
        assertEquals("529.982.247-25", byCpf.get(0).getCpf());
    }

    @Test
    void rejectsInvalidAndDuplicateCpfs() throws IOException {
        CostumerService service = new CostumerService(temporaryDirectory.resolve("clients.db"));

        assertThrows(IllegalArgumentException.class, () -> service.save("Cliente", "111.111.111-11"));
        service.save("Cliente", "529.982.247-25");
        assertThrows(IllegalArgumentException.class, () -> service.save("Outro nome", "52998224725"));
    }

    @Test
    void allowsCpfWithoutNameAndUsesCpfAsDisplayName() throws IOException {
        CostumerService service = new CostumerService(temporaryDirectory.resolve("clients.db"));
        service.save("", "529.982.247-25");

        Costumer customer = service.search("529982").get(0);

        assertEquals("", customer.getName());
        assertEquals("CPF 529.982.247-25", customer.toString());
    }

    @Test
    void updatesCustomerNameAndCpf() throws IOException {
        CostumerService service = new CostumerService(temporaryDirectory.resolve("clients.db"));
        service.save("João", "529.982.247-25");
        Costumer original = service.search("529982").get(0);

        service.update(original, "Maria", "111.444.777-35");

        assertEquals(0, service.search("João").size());
        Costumer updated = service.search("111444").get(0);
        assertEquals("Maria", updated.getName());
        assertEquals("111.444.777-35", updated.getCpf());
    }

    @Test
    void rejectsUpdateUsingAnotherCustomersCpf() throws IOException {
        CostumerService service = new CostumerService(temporaryDirectory.resolve("clients.db"));
        service.save("João", "529.982.247-25");
        service.save("Maria", "111.444.777-35");
        Costumer joao = service.search("João").get(0);

        assertThrows(IllegalArgumentException.class,
            () -> service.update(joao, "João da Silva", "111.444.777-35"));
        assertEquals("João", service.search("529982").get(0).getName());
    }

    @Test
    void migratesExistingCustomerDatabaseWithoutLosingRecords() throws Exception {
        Path database = temporaryDirectory.resolve("legacy.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE customers ("
                + "cpf TEXT PRIMARY KEY NOT NULL, "
                + "name TEXT NOT NULL CHECK (length(trim(name)) > 0))");
            statement.executeUpdate("INSERT INTO customers (cpf, name) "
                + "VALUES ('52998224725', 'João da Silva')");
        }

        CostumerService service = new CostumerService(database);
        service.save(null, "111.444.777-35");

        assertEquals(2, service.list().size());
        assertEquals("João da Silva", service.search("joao").get(0).getName());
        assertEquals("", service.search("11144477735").get(0).getName());
    }
}
