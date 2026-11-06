package com.fritz.lancador.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

public class SefazService {
    public boolean isConfigured() {
        return false;
    }

    public CertificateInfo validateCertificate(Path certificatePath, char[] password)
        throws IOException, GeneralSecurityException {
        if (certificatePath == null || !Files.isRegularFile(certificatePath) || !Files.isReadable(certificatePath)) {
            throw new IOException("O arquivo do certificado não existe ou não pode ser lido.");
        }

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(certificatePath)) {
            keyStore.load(input, password);
        }

        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (!keyStore.isKeyEntry(alias)) {
                continue;
            }

            Key key = keyStore.getKey(alias, password);
            Certificate certificate = keyStore.getCertificate(alias);
            if (!(key instanceof PrivateKey) || !(certificate instanceof X509Certificate)) {
                continue;
            }

            X509Certificate x509Certificate = (X509Certificate) certificate;
            x509Certificate.checkValidity(new Date());
            return new CertificateInfo(
                x509Certificate.getSubjectX500Principal().getName(),
                x509Certificate.getNotAfter());
        }

        throw new CertificateException("O arquivo não contém uma chave privada e um certificado X.509.");
    }

    public static final class CertificateInfo {
        private final String subject;
        private final Date expirationDate;

        private CertificateInfo(String subject, Date expirationDate) {
            this.subject = subject;
            this.expirationDate = new Date(expirationDate.getTime());
        }

        public String getSubject() {
            return subject;
        }

        public Date getExpirationDate() {
            return new Date(expirationDate.getTime());
        }
    }
}
