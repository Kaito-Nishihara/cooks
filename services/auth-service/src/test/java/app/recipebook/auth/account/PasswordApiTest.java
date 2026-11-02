package app.recipebook.auth.account;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import app.recipebook.auth.support.AuthApi;
import app.recipebook.auth.support.MutableClock;
import app.recipebook.auth.support.TestDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** パスワードの変更の API(「機能設計: 登録とログイン」の 4.7、「共通設計: 認証」の 4.9)。 */
@QuarkusTest
class PasswordApiTest {

    private static final String NEW_PASSWORD = "あたらしいあいことばにかえる2027";
    private static final String WRONG_PASSWORD = "まちがったあいことば9999";

    @Inject
    MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.resetToNow();
        TestDatabase.truncateAll();
    }

    private static Response change(RequestSpecification request, String current, String next) {
        return request.contentType(ContentType.JSON.withCharset("UTF-8"))
                .body(AuthApi.body("currentPassword", current, "newPassword", next))
                .put("/api/me/password");
    }

    private static String storedHash() {
        return TestDatabase.queryStrings("SELECT password_hash FROM users ORDER BY created_at").get(0);
    }

    private static void verifyStatus(String cookie, int status) {
        given().cookie(AuthApi.COOKIE, cookie).get("/internal/auth/verify/api/me").then().statusCode(status);
    }

    @Test
    @DisplayName("AC-AUTH-39 正しい今のパスワードと、12 文字の新しいパスワードで送ると、200 と、GET /api/me と同じ本文が返る")
    void changePasswordReturnsMe() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        String me = AuthApi.signedIn(cookie).get("/api/me").asString();

        Response response = change(AuthApi.signedIn(cookie), AuthApi.PASSWORD, "abcdefghijkl");

        response.then().statusCode(200);
        assertThat(response.asString()).isEqualTo(me);
        assertThat(response.getHeader("Set-Cookie")).isNull();
    }

    @Test
    @DisplayName("AC-AUTH-40 AC-AUTH-41 変えた後は、前のパスワードでログインできず、新しいパスワードでログインできる")
    void onlyNewPasswordWorksAfterChange() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        change(AuthApi.signedIn(cookie), AuthApi.PASSWORD, NEW_PASSWORD).then().statusCode(200);

        AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(401).body("code", equalTo("LOGIN_FAILED"));
        AuthApi.login(AuthApi.EMAIL, NEW_PASSWORD).then().statusCode(200);
    }

    @Test
    @DisplayName("AC-AUTH-42 誤った今のパスワードで送ると、401 と LOGIN_FAILED が返り、パスワードのハッシュが変わらない")
    void wrongCurrentPasswordIsRejected() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        String before = storedHash();

        change(AuthApi.signedIn(cookie), WRONG_PASSWORD, NEW_PASSWORD).then().statusCode(401)
                .body("code", equalTo("LOGIN_FAILED"));

        assertThat(storedHash()).isEqualTo(before);
        // ログインの失敗と同じ回数に数える
        assertThat(TestDatabase.count("login_attempts")).isEqualTo(1);
        // ログインし直すダイアログを出させないために、UNAUTHENTICATED にしない
        verifyStatus(cookie, 200);
    }

    @Test
    @DisplayName("AC-AUTH-43 新しいパスワードを 11 文字にすると、400 が返り、fields に newPassword と TOO_SHORT の 1 件が入る")
    void shortNewPasswordIsRejected() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);

        change(AuthApi.signedIn(cookie), AuthApi.PASSWORD, "abcdefghi12").then().statusCode(400)
                .body("details.fields", hasSize(1))
                .body("details.fields[0].path", equalTo("newPassword"))
                .body("details.fields[0].code", equalTo("TOO_SHORT"));
    }

    @Test
    @DisplayName("AC-AUTH-44 誤った今のパスワードで 5 回送った後、正しい今のパスワードで送ると、429 と LOGIN_LOCKED が返り、ハッシュが変わらない")
    void lockedAfterFiveWrongCurrentPasswords() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        String before = storedHash();
        for (int i = 0; i < 5; i++) {
            change(AuthApi.signedIn(cookie), WRONG_PASSWORD, NEW_PASSWORD).then().statusCode(401);
        }

        change(AuthApi.signedIn(cookie), AuthApi.PASSWORD, NEW_PASSWORD).then().statusCode(429)
                .body("code", equalTo("LOGIN_LOCKED"))
                .body("details.retryAfterSeconds", equalTo(900));

        assertThat(storedHash()).isEqualTo(before);
        // 受け付けない間は、ログインもできない
        AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(429);
    }

    @Test
    @DisplayName("AC-AUTH-45 利用者 A のトークンと、利用者 B のクッキーを付けて呼ぶと、401 が返り、どちらのハッシュも変わらない")
    void tokenAndCookieMustBelongToSameUser() {
        String misaki = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        String mother = AuthApi.register("おかあさん", "haha@example.com", AuthApi.PASSWORD).getCookie(AuthApi.COOKIE);
        String before = String.join(",", TestDatabase.queryStrings("SELECT password_hash FROM users ORDER BY email"));

        RequestSpecification mixed = given().header("Authorization", "Bearer " + AuthApi.tokenFor(misaki))
                .cookie(AuthApi.COOKIE, mother);
        change(mixed, AuthApi.PASSWORD, NEW_PASSWORD).then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));

        assertThat(String.join(",", TestDatabase.queryStrings("SELECT password_hash FROM users ORDER BY email")))
                .isEqualTo(before);
    }

    @Test
    @DisplayName("AC-AUTH-46 今のパスワードと同じ値を新しいパスワードにして送ると、200 が返る")
    void samePasswordIsAllowed() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);

        change(AuthApi.signedIn(cookie), AuthApi.PASSWORD, AuthApi.PASSWORD).then().statusCode(200);
    }

    @Test
    @DisplayName("AC-AUTHN-20 パスワードを変えると、変えた端末のセッションは 200 のままで、同じ人のほかのセッションは 401 を返す")
    void changeEndsOtherSessions() {
        String phone = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        String laptop = AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).getCookie(AuthApi.COOKIE);
        String mother = AuthApi.register("おかあさん", "haha@example.com", AuthApi.PASSWORD).getCookie(AuthApi.COOKIE);

        change(AuthApi.signedIn(phone), AuthApi.PASSWORD, NEW_PASSWORD).then().statusCode(200);

        verifyStatus(phone, 200);
        verifyStatus(laptop, 401);
        // ほかの人のセッションには触れない
        verifyStatus(mother, 200);
    }

    @Test
    @DisplayName("トークンがなければ、401 と UNAUTHENTICATED が返る")
    void changeWithoutTokenIsUnauthenticated() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);

        change(given().cookie(AuthApi.COOKIE, cookie), AuthApi.PASSWORD, NEW_PASSWORD).then().statusCode(401)
                .body("code", equalTo("UNAUTHENTICATED"));
    }
}
