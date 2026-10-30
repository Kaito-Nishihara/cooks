package app.recipebook.auth.token.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 署名の鍵の読み込み。Quarkus を起動しない。 */
class SigningKeysTest {

    @TempDir
    Path directory;

    private static SigningKeys keysFrom(Path file) {
        return new SigningKeys(file.toString(), List.of());
    }

    @Test
    @DisplayName("鍵のファイルがないと、要求を受けられない状態になり、公開の鍵の一覧が空になる")
    void missingFileIsNotReady() {
        SigningKeys keys = keysFrom(directory.resolve("none.pem"));

        assertThat(keys.isReady()).isFalse();
        assertThat(keys.publish()).isEmpty();
    }

    @Test
    @DisplayName("鍵のファイルが後から置かれると、起動し直さずに読める")
    void fileAddedLaterIsLoaded() throws Exception {
        Path file = directory.resolve("later.pem");
        SigningKeys keys = keysFrom(file);
        assertThat(keys.isReady()).isFalse();

        writePrivateKey(file, 2048);

        assertThat(keys.isReady()).isTrue();
        assertThat(keys.publish()).hasSize(1);
    }

    @Test
    @DisplayName("2,048 ビットより短い鍵は、受け付けない")
    void shortKeyIsRejected() throws Exception {
        Path file = directory.resolve("short.pem");
        writePrivateKey(file, 1024);

        assertThat(keysFrom(file).isReady()).isFalse();
    }

    private static void writePrivateKey(Path file, int bits) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(bits);
        byte[] der = generator.generateKeyPair().getPrivate().getEncoded();
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        Files.writeString(file, "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n");
    }
}
