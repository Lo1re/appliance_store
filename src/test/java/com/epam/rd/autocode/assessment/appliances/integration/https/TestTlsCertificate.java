package com.epam.rd.autocode.assessment.appliances.integration.https;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

// Test-only localhost certificate; no personal keystore or operating-system trust changes.
final class TestTlsCertificate {
    static final String PASSWORD = "test-certificate-only-2026";
    static final Path KEYSTORE = create();

    private TestTlsCertificate() {}

    private static Path create() {
        try {
            Path directory = Files.createTempDirectory("store-tls-test-");
            Path store = directory.resolve("localhost.p12");
            directory.toFile().deleteOnExit();
            store.toFile().deleteOnExit();
            String executable =
                    System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
            String keytool = Path.of(System.getProperty("java.home"), "bin", executable).toString();
            var process =
                    new ProcessBuilder(
                                    keytool,
                                    "-genkeypair",
                                    "-alias",
                                    "appliance-store",
                                    "-keyalg",
                                    "RSA",
                                    "-keysize",
                                    "2048",
                                    "-storetype",
                                    "PKCS12",
                                    "-keystore",
                                    store.toString(),
                                    "-storepass",
                                    PASSWORD,
                                    "-validity",
                                    "2",
                                    "-dname",
                                    "CN=localhost",
                                    "-ext",
                                    "SAN=dns:localhost,ip:127.0.0.1,ip:::1",
                                    "-ext",
                                    "EKU=serverAuth")
                            .redirectErrorStream(true)
                            .start();
            String output =
                    new String(
                            process.getInputStream().readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8);
            if (process.waitFor() != 0) {
                throw new IllegalStateException("Test certificate generation failed: " + output);
            }
            return store;
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot create test-only TLS certificate", failure);
        }
    }

    static SSLContext trustOnlyTestCertificate() throws Exception {
        var store = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(KEYSTORE)) {
            store.load(input, PASSWORD.toCharArray());
        }
        var trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(store);
        var context = SSLContext.getInstance("TLS");
        context.init(null, trust.getTrustManagers(), null);
        return context;
    }
}
