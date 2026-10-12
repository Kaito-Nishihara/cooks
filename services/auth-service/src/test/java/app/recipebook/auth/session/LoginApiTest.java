package app.recipebook.auth.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;

import app.recipebook.auth.support.AuthApi;
import app.recipebook.auth.support.MutableClock;
import app.recipebook.auth.support.TestDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ログインの API(「機能設計: 登録とログイン」の 4.4、「共通設計: 認証」の 4.3 と 4.4)。 */
@QuarkusTest
class LoginApiTest {

    private static final String WRONG_PASSWORD = "まちがったあいことば9999";

    @Inject
    MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.reset();
        TestDatabase.truncateAll();
    }

    private static void failLogin(String email, int times) {
        for (int i = 0; i < times; i++) {
            AuthApi.login(email, WRONG_PASSWORD).then().statusCode(401).body("code", equalTo("LOGIN_FAILED"));
        }
    }

    @Test
    @DisplayName("AC-AUTH-28 正しいメールアドレスとパスワードでログインすると、200 が返り、本文が 3 項目で、Set-Cookie に rb_session がある")
    void loginReturnsUserAndCookie() {
        Response registered = AuthApi.registerMisaki();

        Response response = AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD);

        response.then().statusCode(200)
                .body("size()", equalTo(3))
                .body("id", equalTo(registered.jsonPath().getString("id")))
                .body("email", equalTo("misaki@example.com"))
                .body("displayName", equalTo("みさき"));
        assertThat(response.getCookie(AuthApi.COOKIE)).hasSize(43);
    }

    @Test
    @DisplayName("AC-AUTHN-01 ログインに成功した応答の Set-Cookie に、決まった属性が含まれ、Domain が含まれない")
    void cookieHasRequiredAttributes() {
        AuthApi.registerMisaki();

        String setCookie = AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).getHeader("Set-Cookie");

        assertThat(setCookie)
                .startsWith("rb_session=")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax")
                .contains("Path=/")
                .contains("Max-Age=2592000")
                .doesNotContainIgnoringCase("Domain");
    }

    @Test
    @DisplayName("AC-AUTHN-22 クッキーなしで POST /api/auth/login を呼べる")
    void loginNeedsNoCookie() {
        AuthApi.registerMisaki();
        AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(200);
    }

    @Test
    @DisplayName("AC-AUTHN-03 AC-AUTH-05 登録してログインした後、どのテーブルのどの列にも、パスワードとクッキーの値が、そのままの形で入っていない")
    void secretsAreNotStoredAsIs() {
        String firstCookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        String secondCookie = AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).getCookie(AuthApi.COOKIE);
        failLogin(AuthApi.EMAIL, 1);

        String stored = TestDatabase.dumpAll();
        assertThat(TestDatabase.count("sessions")).isEqualTo(2);
        assertThat(TestDatabase.count("login_attempts")).isEqualTo(1);
        assertThat(stored)
                .doesNotContain(AuthApi.PASSWORD)
                .doesNotContain(WRONG_PASSWORD)
                .doesNotContain(firstCookie)
                .doesNotContain(secondCookie);
        // ログインの失敗の記録に、メールアドレスそのものを残さない
        assertThat(TestDatabase.queryStrings("SELECT row_to_json(t)::text FROM login_attempts t").get(0))
                .doesNotContain("misaki");
    }

    @Test
    @DisplayName("AC-AUTHN-05 AC-AUTH-08 誤ったパスワードでのログインと、登録のないメールアドレスでのログインとで、状態と本文が同じである")
    void failureDoesNotRevealRegistration() {
        AuthApi.registerMisaki();

        Response wrongPassword = AuthApi.login(AuthApi.EMAIL, WRONG_PASSWORD);
        Response unknownEmail = AuthApi.login("daremo@example.com", WRONG_PASSWORD);

        assertThat(wrongPassword.statusCode()).isEqualTo(401);
        assertThat(unknownEmail.statusCode()).isEqualTo(401);
        assertThat(unknownEmail.asString()).isEqualTo(wrongPassword.asString());
        wrongPassword.then()
                .body("code", equalTo("LOGIN_FAILED"))
                .body("message", equalTo("メールアドレスかパスワードが違います。"))
                .body("details", nullValue());
        assertThat(wrongPassword.getCookie(AuthApi.COOKIE)).isNull();
    }

    @Test
    @DisplayName("AC-AUTH-29 大文字と小文字が違うメールアドレスでも、正しいパスワードでログインできる")
    void loginIgnoresCaseOfEmail() {
        AuthApi.registerMisaki();
        AuthApi.login("Misaki@Example.com", AuthApi.PASSWORD).then().statusCode(200);
    }

    @Test
    @DisplayName("AC-AUTH-30 同じメールアドレスで、誤ったパスワードのログインを 5 回送ると、5 回とも 401 と LOGIN_FAILED が返る")
    void fiveFailuresAreAllLoginFailed() {
        AuthApi.registerMisaki();
        failLogin(AuthApi.EMAIL, 5);
    }

    @Test
    @DisplayName("AC-AUTHN-06 5 回失敗した後、正しいパスワードでログインすると、429 と LOGIN_LOCKED が返り、retryAfterSeconds が 1 以上 900 以下である")
    void lockedAfterFiveFailures() {
        AuthApi.registerMisaki();
        failLogin(AuthApi.EMAIL, 5);

        AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(429)
                .body("code", equalTo("LOGIN_LOCKED"))
                .body("message", equalTo("ログインの失敗が続いたため、しばらく受け付けません。"))
                .body("details.retryAfterSeconds", equalTo(900));

        clock.advance(Duration.ofSeconds(59).plusMillis(500));
        AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(429)
                .body("details.retryAfterSeconds", allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(900)))
                .body("details.retryAfterSeconds", equalTo(841));
    }

    @Test
    @DisplayName("AC-AUTHN-07 登録のないメールアドレスで 5 回失敗した後の 6 回目も、429 と LOGIN_LOCKED が返る")
    void unknownEmailIsLockedTheSameWay() {
        failLogin("daremo@example.com", 5);

        AuthApi.login("daremo@example.com", WRONG_PASSWORD).then().statusCode(429)
                .body("code", equalTo("LOGIN_LOCKED"));
    }

    @Test
    @DisplayName("AC-AUTHN-08 受け付けない期限を過ぎると、正しいパスワードでログインできる")
    void lockExpires() {
        AuthApi.registerMisaki();
        failLogin(AuthApi.EMAIL, 5);

        clock.advance(Duration.ofMinutes(15).minusSeconds(1));
        AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(429)
                .body("details.retryAfterSeconds", equalTo(1));

        clock.advance(Duration.ofSeconds(1));
        AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(200);
        assertThat(TestDatabase.count("login_attempts")).isZero();
    }

    @Test
    @DisplayName("受け付けない期限を過ぎた後は、また 5 回まで試せる")
    void failuresAreCountedAgainAfterLock() {
        failLogin(AuthApi.EMAIL, 5);
        clock.advance(Duration.ofMinutes(15));

        failLogin(AuthApi.EMAIL, 5);
        AuthApi.login(AuthApi.EMAIL, WRONG_PASSWORD).then().statusCode(429);
    }

    @Test
    @DisplayName("15 分より前の失敗は、回数に数えない")
    void oldFailuresAreNotCounted() {
        AuthApi.registerMisaki();
        failLogin(AuthApi.EMAIL, 4);
        clock.advance(Duration.ofMinutes(15));

        failLogin(AuthApi.EMAIL, 4);
        AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(200);
    }

    @Test
    @DisplayName("AC-AUTH-31 4 回失敗した後に正しいパスワードでログインし、続けて 4 回失敗させると、4 回とも 401 と LOGIN_FAILED が返る")
    void successResetsFailureCount() {
        AuthApi.registerMisaki();
        failLogin(AuthApi.EMAIL, 4);
        AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(200);

        failLogin(AuthApi.EMAIL, 4);
    }

    @Test
    @DisplayName("AC-AUTH-32 メールアドレスを空にしたログインを 6 回送ると、6 回とも 400 が返り、login_attempts に行ができない")
    void invalidInputIsNotCounted() {
        for (int i = 0; i < 6; i++) {
            AuthApi.login("", AuthApi.PASSWORD).then().statusCode(400)
                    .body("code", equalTo("VALIDATION_FAILED"))
                    .body("details.fields[0].path", equalTo("email"))
                    .body("details.fields[0].code", equalTo("REQUIRED"));
        }
        assertThat(TestDatabase.count("login_attempts")).isZero();
    }

    @Test
    @DisplayName("AC-AUTH-33 パスワードを半角の英数字 11 文字にしてログインすると、400 ではなく、401 と LOGIN_FAILED が返る")
    void shortPasswordIsNotValidationErrorAtLogin() {
        AuthApi.registerMisaki();
        AuthApi.login(AuthApi.EMAIL, "abcdefghi12").then().statusCode(401).body("code", equalTo("LOGIN_FAILED"));
    }

    @Test
    @DisplayName("AC-AUTH-23 全角の「Password2026!」で登録した後、半角の「Password2026!」でログインすると、200 が返る")
    void passwordIsNormalized() {
        AuthApi.register(AuthApi.NAME, AuthApi.EMAIL, "Ｐａｓｓｗｏｒｄ２０２６！").then().statusCode(201);
        AuthApi.login(AuthApi.EMAIL, "Password2026!").then().statusCode(200);
    }

    @Test
    @DisplayName("AC-AUTH-24 先頭に空白を付けたパスワードで登録した後、空白を除いた値でログインすると、401 と LOGIN_FAILED が返る")
    void passwordIsNotTrimmed() {
        AuthApi.register(AuthApi.NAME, AuthApi.EMAIL, " abcdefghijkl").then().statusCode(201);

        AuthApi.login(AuthApi.EMAIL, "abcdefghijkl").then().statusCode(401).body("code", equalTo("LOGIN_FAILED"));
        AuthApi.login(AuthApi.EMAIL, " abcdefghijkl").then().statusCode(200);
    }

    @Test
    @DisplayName("AC-AUTH-34 ログインを 2 回続けて行うと、2 回の Set-Cookie の rb_session の値が違う")
    void everyLoginIssuesNewToken() {
        AuthApi.registerMisaki();

        String first = AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).getCookie(AuthApi.COOKIE);
        String second = AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).getCookie(AuthApi.COOKIE);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("AC-AUTH-35 1 回目のログインのクッキーを付けて 2 回目のログインをすると、1 回目のセッションがなくなる(セッションの行で確かめる)")
    void loginWithCookieRemovesPreviousSession() {
        String registered = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        assertThat(TestDatabase.count("sessions")).isEqualTo(1);
        String firstHash = TestDatabase.queryStrings("SELECT token_hash FROM sessions").get(0);

        AuthApi.fromOwnSite().cookie(AuthApi.COOKIE, registered)
                .body(AuthApi.body("email", AuthApi.EMAIL, "password", AuthApi.PASSWORD))
                .when().post("/api/auth/login").then().statusCode(200);

        assertThat(TestDatabase.queryStrings("SELECT token_hash FROM sessions")).hasSize(1).doesNotContain(firstHash);
    }

    @Test
    @DisplayName("形が合わないクッキーが付いていても、ログインできる")
    void malformedCookieIsIgnored() {
        AuthApi.registerMisaki();
        AuthApi.fromOwnSite().cookie(AuthApi.COOKIE, "こわれた値")
                .body(AuthApi.body("email", AuthApi.EMAIL, "password", AuthApi.PASSWORD))
                .when().post("/api/auth/login").then().statusCode(200);
    }

    @Test
    @DisplayName("AC-AUTHN-14 Origin がほかのサイトの POST /api/auth/login は、正しいパスワードでも 403 が返り、セッションが作られない")
    void crossSiteLoginIsRejected() {
        AuthApi.registerMisaki();
        int before = TestDatabase.count("sessions");

        RestAssured.given().header("Origin", "https://evil.example").contentType(ContentType.JSON.withCharset("UTF-8"))
                .body(AuthApi.body("email", AuthApi.EMAIL, "password", AuthApi.PASSWORD))
                .when().post("/api/auth/login")
                .then().statusCode(403).body("code", equalTo("CROSS_SITE_REQUEST"));

        assertThat(TestDatabase.count("sessions")).isEqualTo(before);
    }

    @Test
    @DisplayName("11 回ログインすると、セッションは 10 個で、最初のセッションがなくなる")
    void sessionsAreLimitedToTen() {
        AuthApi.registerMisaki();
        String firstHash = TestDatabase.queryStrings("SELECT token_hash FROM sessions").get(0);

        for (int i = 0; i < 10; i++) {
            clock.advance(Duration.ofSeconds(1));
            AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(200);
        }

        assertThat(TestDatabase.queryStrings("SELECT token_hash FROM sessions")).hasSize(10).doesNotContain(firstHash);
    }

    @Test
    @DisplayName("セッションの期限は、作った瞬間の 30 日後である")
    void sessionExpiresInThirtyDays() {
        AuthApi.registerMisaki();

        assertThat(TestDatabase.queryStrings(
                "SELECT to_char(expires_at AT TIME ZONE 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') FROM sessions"))
                .containsExactly("2026-11-09T03:00:00Z");
    }
}
