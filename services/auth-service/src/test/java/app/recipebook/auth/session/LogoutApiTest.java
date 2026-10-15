package app.recipebook.auth.session;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import app.recipebook.auth.support.AuthApi;
import app.recipebook.auth.support.MutableClock;
import app.recipebook.auth.support.TestDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** ログアウトの API(「機能設計: 登録とログイン」の 4.5、「共通設計: 認証」の 4.9)。 */
@QuarkusTest
class LogoutApiTest {

    @Inject
    MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.resetToNow();
        TestDatabase.truncateAll();
    }

    private static void verifyStatus(String cookie, int status) {
        given().cookie(AuthApi.COOKIE, cookie).get("/internal/auth/verify/api/me").then().statusCode(status);
    }

    @Test
    @DisplayName("AC-AUTH-36 POST /api/auth/logout が 204 を返し、Set-Cookie に rb_session と Max-Age=0 と HttpOnly と Path=/ が含まれる")
    void logoutClearsCookie() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);

        Response response = AuthApi.signedIn(cookie).post("/api/auth/logout");

        response.then().statusCode(204);
        assertThat(response.asString()).isEmpty();
        assertThat(response.getHeader("Set-Cookie"))
                .startsWith("rb_session=")
                .contains("Max-Age=0")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax")
                .contains("Path=/");
    }

    @Test
    @DisplayName("AC-AUTHN-19 ログアウトの後、同じクッキーで verify を呼ぶと 401 が返る。同じ人のほかのセッションは 200 を返す")
    void logoutEndsOnlyCurrentSession() {
        String first = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        String second = AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).getCookie(AuthApi.COOKIE);

        AuthApi.signedIn(first).post("/api/auth/logout").then().statusCode(204);

        verifyStatus(first, 401);
        verifyStatus(second, 200);
    }

    @Test
    @DisplayName("AC-AUTH-37 トークンを付けずに POST /api/auth/logout を呼ぶと、401 と UNAUTHENTICATED が返る")
    void logoutWithoutTokenIsUnauthenticated() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);

        given().cookie(AuthApi.COOKIE, cookie).post("/api/auth/logout")
                .then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
        verifyStatus(cookie, 200);
    }

    @Test
    @DisplayName("セッションの行がもうないときも、204 とクッキーを消す指示を返す")
    void logoutOfMissingSessionSucceeds() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        // 先にトークンを取っておく。ログアウトの後は、verify でトークンを取れない
        String token = AuthApi.tokenFor(cookie);
        AuthApi.signedIn(cookie).post("/api/auth/logout").then().statusCode(204);

        given().header("Authorization", "Bearer " + token).cookie(AuthApi.COOKIE, cookie)
                .post("/api/auth/logout").then().statusCode(204);
    }

    @Test
    @DisplayName("ほかの人のセッションのクッキーでは、ログアウトできず、401 が返る")
    void cannotLogoutOtherUsersSession() {
        String misaki = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        String mother = AuthApi.register("おかあさん", "haha@example.com", AuthApi.PASSWORD).getCookie(AuthApi.COOKIE);

        given().header("Authorization", "Bearer " + AuthApi.tokenFor(misaki)).cookie(AuthApi.COOKIE, mother)
                .post("/api/auth/logout")
                .then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
        verifyStatus(mother, 200);
    }

    @Test
    @DisplayName("AC-AUTH-27 利用者 A のクッキーを付けて利用者 B を登録すると、A のクッキーでは本人を確かめられない")
    void registeringAnotherUserEndsPreviousSession() {
        String cookieOfA = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);

        AuthApi.fromOwnSite().cookie(AuthApi.COOKIE, cookieOfA)
                .body(AuthApi.body("displayName", "おかあさん", "email", "haha@example.com", "password", AuthApi.PASSWORD))
                .post("/api/auth/register").then().statusCode(201);

        // 入口は、verify が 401 を返すと、GET /api/me へ渡さずに 401 を返す
        verifyStatus(cookieOfA, 401);
    }

    @Test
    @DisplayName("AC-AUTH-35 1 回目のログインのクッキーを付けて 2 回目のログインをすると、1 回目のクッキーは 401、2 回目は 200 になる")
    void secondLoginEndsFirstSession() {
        AuthApi.registerMisaki();
        String first = AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).getCookie(AuthApi.COOKIE);

        String second = AuthApi.fromOwnSite().cookie(AuthApi.COOKIE, first)
                .body(AuthApi.body("email", AuthApi.EMAIL, "password", AuthApi.PASSWORD))
                .post("/api/auth/login").then().statusCode(200).extract().cookie(AuthApi.COOKIE);

        verifyStatus(first, 401);
        AuthApi.signedIn(second).get("/api/me").then().statusCode(200);
    }
}
