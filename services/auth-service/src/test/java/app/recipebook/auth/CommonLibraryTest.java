package app.recipebook.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 共通のライブラリの部品が、このサービスでも効いていることを確かめる。 */
@QuarkusTest
class CommonLibraryTest {

    @Test
    @DisplayName("AC-API-01 ないパスを呼ぶと 404 と NOT_FOUND が返り、details が null である")
    void unknownPathReturnsNotFound() {
        given().header("X-Request-Id", "auth-404")
                .when().get("/api/no-such-path")
                .then().statusCode(404)
                .header("X-Request-Id", equalTo("auth-404"))
                .body("code", equalTo("NOT_FOUND"))
                .body("details", nullValue());
    }
}
