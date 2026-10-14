package app.recipebook.auth.support;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

/**
 * テストの開始のときに、署名の鍵の組を作り、ファイルに書いて、その場所を設定に渡す。
 * 鍵をリポジトリに置かないためである(「共通設計: サービスの共通構成」の 7 章)。
 * 入れ替えの間に残す古い鍵も 1 つ作る。
 */
public class TestSigningKeys implements QuarkusTestResourceLifecycleManager {

    private Path directory;

    @Override
    public Map<String, String> start() {
        try {
            directory = Files.createTempDirectory("signing-keys");
            Path current = directory.resolve("current.pem");
            Path retired = directory.resolve("retired.pub.pem");
            Files.writeString(current, pem("PRIVATE KEY", newKeyPair().getPrivate().getEncoded()), StandardCharsets.US_ASCII);
            Files.writeString(retired, pem("PUBLIC KEY", newKeyPair().getPublic().getEncoded()), StandardCharsets.US_ASCII);
            return Map.of(
                    "app.auth.signing-key-file", current.toString(),
                    "app.auth.retired-public-key-files", retired.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void stop() {
        if (directory == null) {
            return;
        }
        try (var files = Files.list(directory)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
            Files.deleteIfExists(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static KeyPair newKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String pem(String label, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n";
    }
}
