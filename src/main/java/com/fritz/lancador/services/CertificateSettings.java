package com.fritz.lancador.services;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class CertificateSettings {
    private static final String CERTIFICATE_PATH_KEY = "a1-certificate-path";
    private final Preferences preferences = Preferences.userNodeForPackage(CertificateSettings.class);

    public String getCertificatePath() {
        return preferences.get(CERTIFICATE_PATH_KEY, "");
    }

    public void saveCertificatePath(String path) throws BackingStoreException {
        preferences.put(CERTIFICATE_PATH_KEY, path);
        preferences.flush();
    }
}
