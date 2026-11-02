package app.recipebook.auth;

import static org.assertj.core.api.Assertions.assertThat;

import app.recipebook.auth.support.AuthApi;
import app.recipebook.auth.support.MutableClock;
import app.recipebook.auth.support.TestDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ログに秘密の値と個人の情報を出さないこと(「機能設計: 登録とログイン」の 7 章)。 */
@QuarkusTest
class SecretLogTest {

    private static final Path LOG = Path.of("target/test-logs/app.json");

    @Inject
    MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.resetToNow();
        TestDatabase.truncateAll();
    }

    private static String logText() throws IOException {
        return Files.exists(LOG) ? Files.readString(LOG, StandardCharsets.UTF_8) : "";
    }

    @Test
    @DisplayName("AC-AUTH-51 登録、ログイン、誤ったパスワードでのログイン、パスワードの変更の後、ログに、メールアドレス、表示名、パスワード、クッキーの値のどれも含まれない")
    void secretsAreNotLogged() throws Exception {
        String email = "secret-" + System.nanoTime() + "@example.com";
        String name = "ひみつのなまえ" + System.nanoTime();
        String password = "ひみつのあいことば" + System.nanoTime();
        String newPassword = "あたらしいひみつ" + System.nanoTime();

        String cookie = AuthApi.register(name, email, password).then().statusCode(201).extract().cookie(AuthApi.COOKIE);
        String loginCookie = AuthApi.login(email, password).then().statusCode(200).extract().cookie(AuthApi.COOKIE);
        AuthApi.login(email, "まちがったあいことば9999").then().statusCode(401);
        AuthApi.signedIn(loginCookie).contentType(ContentType.JSON.withCharset("UTF-8"))
                .body(AuthApi.body("currentPassword", password, "newPassword", newPassword))
                .put("/api/me/password").then().statusCode(200);

        // 最後の要求の記録が書かれるまで待つ
        Awaitility.await().atMost(Duration.ofSeconds(5))
                .until(() -> logText().contains("/api/me/password"));
        assertThat(logText())
                .doesNotContain(email)
                .doesNotContain(name)
                .doesNotContain(password)
                .doesNotContain(newPassword)
                .doesNotContain(cookie)
                .doesNotContain(loginCookie);
    }
}
