package com.fritz.lancador.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SefazServiceTest {
    @TempDir
    Path temporaryDirectory;

    private final SefazService service = new SefazService();

    @Test
    void rejectsMissingCertificateFile() {
        assertThrows(IOException.class,
            () -> service.validateCertificate(temporaryDirectory.resolve("missing.p12"), new char[0]));
    }

    @Test
    void rejectsNonPkcs12File() throws IOException {
        Path invalidCertificate = temporaryDirectory.resolve("invalid.p12");
        Files.write(invalidCertificate, "not a certificate".getBytes("UTF-8"));

        assertThrows(IOException.class,
            () -> service.validateCertificate(invalidCertificate, "password".toCharArray()));
    }
}
