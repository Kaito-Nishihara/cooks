package app.recipebook.auth.account;

import static io.restassured.RestAssured.given;
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

/** 自分の情報の API(「機能設計: 登録とログイン」の 4.6)。 */
@QuarkusTest
class MeApiTest {

    @Inject
    MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.resetToNow();
        TestDatabase.truncateAll();
    }

    @Test
    @DisplayName("AC-AUTH-10 登録で返ったクッキーで GET /api/me を呼ぶと、200 と、登録の応答と同じ本文が返る")
    void meReturnsSameBodyAsRegistration() {
        Response registered = AuthApi.registerMisaki();

        String body = AuthApi.signedIn(registered.getCookie(AuthApi.COOKIE)).get("/api/me")
                .then().statusCode(200).extract().asString();

        org.assertj.core.api.Assertions.assertThat(body).isEqualTo(registered.asString());
    }

    @Test
    @DisplayName("AC-AUTH-38 トークンを付けずに GET /api/me を呼ぶと、401 と UNAUTHENTICATED が返る")
    void meWithoutTokenIsUnauthenticated() {
        given().get("/api/me").then().statusCode(401)
                .body("code", equalTo("UNAUTHENTICATED"))
                .body("message", equalTo("ログインが必要です。"));
    }

    @Test
    @DisplayName("トークンの利用者がもういないときは、401 と UNAUTHENTICATED が返る")
    void meForDeletedUserIsUnauthenticated() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        String token = AuthApi.tokenFor(cookie);
        TestDatabase.truncateAll();

        given().header("Authorization", "Bearer " + token).get("/api/me")
                .then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
    }
}
