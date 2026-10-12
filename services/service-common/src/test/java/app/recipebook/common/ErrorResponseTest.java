package app.recipebook.common;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;

import app.recipebook.common.probe.ProbeResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** エラーの応答の形を確かめる(「共通設計: API の共通仕様」の 4.7 から 4.9)。 */
@QuarkusTest
class ErrorResponseTest {

    private static final String VALID_BODY = """
            {"name":"味噌汁","email":"probe@example.com","password":"p","rows":[{"amount":1}]}""";

    @Test
    @DisplayName("前提: 正しい入力は 201 を返す")
    void validRequestIsAccepted() {
        given().contentType(ContentType.JSON).body(VALID_BODY)
                .when().post("/api/probes")
                .then().statusCode(201).body("name", equalTo("味噌汁"));
    }

    @Test
    @DisplayName("AC-API-01 ないパスを呼ぶと 404 と NOT_FOUND が返り、details が null である")
    void unknownPathReturnsNotFound() {
        given().when().get("/api/no-such-path")
                .then().statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("見つかりませんでした。"))
                .body("$", hasKey("details"))
                .body("details", nullValue());
    }

    @Test
    @DisplayName("AC-API-01 /api の外のないパスでも、同じ形の 404 が返る")
    void unknownPathOutsideApiReturnsNotFound() {
        given().when().get("/no-such-root")
                .then().statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", equalTo("NOT_FOUND"))
                .body("$", hasKey("details"))
                .body("details", nullValue());
    }

    @Test
    @DisplayName("パスはあるがメソッドが違うと 404 と NOT_FOUND が返る")
    void wrongMethodReturnsNotFound() {
        given().when().delete("/api/probes")
                .then().statusCode(404).body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @DisplayName("AC-API-02 JSON として読めない本文を送ると 400 と VALIDATION_FAILED が返る")
    void brokenJsonReturnsValidationFailed() {
        given().contentType(ContentType.JSON).body("{\"name\": ")
                .when().post("/api/probes")
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_FAILED"))
                .body("message", equalTo("入力に誤りがあります。"))
                .body("details.fields", hasSize(0));
    }

    @Test
    @DisplayName("Content-Type が JSON でないと 400 と VALIDATION_FAILED が返る")
    void nonJsonContentTypeReturnsValidationFailed() {
        given().contentType(ContentType.TEXT).body("name=x")
                .when().post("/api/probes")
                .then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("AC-API-03 知らない項目を 1 つ足して送ると 400 が返り、fields にその path と UNKNOWN_FIELD が入る")
    void unknownFieldIsRejected() {
        String body = """
                {"name":"味噌汁","email":"probe@example.com","rows":[{"amount":1},{"amount":2,"unit":"g"}]}""";
        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/probes")
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_FAILED"))
                .body("details.fields", hasSize(1))
                .body("details.fields[0].path", equalTo("rows[1].unit"))
                .body("details.fields[0].code", equalTo("UNKNOWN_FIELD"));
    }

    @Test
    @DisplayName("AC-API-04 必須の項目を 2 つ欠いた入力を送ると、fields に 2 件が入る")
    void allViolationsAreReturned() {
        given().contentType(ContentType.JSON).body("{\"password\":\"p\"}")
                .when().post("/api/probes")
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_FAILED"))
                .body("details.fields", hasSize(2))
                .body("details.fields.path", containsInAnyOrder("name", "email"))
                .body("details.fields.code", contains("REQUIRED", "REQUIRED"));
    }

    @Test
    @DisplayName("入力の誤りの path が項目名と添字で、code が違反の種類に対応する")
    void violationPathAndCode() {
        String body = """
                {"name":"123456789012345678901","email":"probe@example.com","rows":[{"amount":1},{"amount":0}]}""";
        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/probes")
                .then().statusCode(400)
                .body("details.fields", hasSize(2))
                .body("details.fields.find { it.path == 'name' }.code", equalTo("TOO_LONG"))
                .body("details.fields.find { it.path == 'rows[1].amount' }.code", equalTo("MIN"));
    }

    @Test
    @DisplayName("配列の件数が上限を超えると TOO_MANY、型が合わない値は INVALID_FORMAT になる")
    void tooManyAndInvalidFormat() {
        String tooMany = """
                {"name":"n","email":"e","rows":[{"amount":1},{"amount":1},{"amount":1},{"amount":1}]}""";
        given().contentType(ContentType.JSON).body(tooMany)
                .when().post("/api/probes")
                .then().statusCode(400)
                .body("details.fields[0].path", equalTo("rows"))
                .body("details.fields[0].code", equalTo("TOO_MANY"));

        String wrongType = """
                {"name":"n","email":"e","rows":[{"amount":"たくさん"}]}""";
        given().contentType(ContentType.JSON).body(wrongType)
                .when().post("/api/probes")
                .then().statusCode(400)
                .body("details.fields[0].path", equalTo("rows[0].amount"))
                .body("details.fields[0].code", equalTo("INVALID_FORMAT"));
    }

    @Test
    @DisplayName("AC-API-05 65KB の本文を送ると 413 と PAYLOAD_TOO_LARGE が返る")
    void largeBodyIsRejected() {
        String body = "{\"name\":\"" + "a".repeat(65 * 1024) + "\",\"email\":\"probe@example.com\"}";
        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/probes")
                .then().statusCode(413)
                .body("code", equalTo("PAYLOAD_TOO_LARGE"))
                .body("details", nullValue());
    }

    @Test
    @DisplayName("AC-API-05 上限ちょうど(64KB)の本文は、大きさでは拒まれない")
    void bodyAtLimitIsNotRejectedBySize() {
        String prefix = "{\"name\":\"";
        String suffix = "\",\"email\":\"probe@example.com\"}";
        String body = prefix + "a".repeat(64 * 1024 - prefix.length() - suffix.length()) + suffix;
        // 名前が長すぎるので 400 になる。413 でなければ、大きさの検査は通っている
        given().contentType(ContentType.JSON).body(body)
                .when().post("/api/probes")
                .then().statusCode(400).body("code", equalTo("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Content-Length のない本文は、1MB を超えると 413 で打ち切られる")
    void bodyWithoutLengthIsCutOff() {
        byte[] body = ("{\"name\":\"" + "a".repeat(2 * 1024 * 1024) + "\"}").getBytes(StandardCharsets.UTF_8);
        // InputStream で渡すと、Content-Length を付けずに、分けて送る
        given().contentType(ContentType.JSON).body(new ByteArrayInputStream(body))
                .when().post("/api/probes")
                .then().statusCode(413);
    }

    @Test
    @DisplayName("AC-API-06 想定していない例外が起きると 500 と INTERNAL_ERROR が返り、例外の名前とスタックトレースを含まない")
    void unexpectedExceptionIsHidden() {
        given().when().get("/api/probes/p-1/crash")
                .then().statusCode(500)
                .body("code", equalTo("INTERNAL_ERROR"))
                .body("details", nullValue())
                .body(not(containsString("IllegalStateException")))
                .body(not(containsString(ProbeResource.CRASH_DETAIL)))
                .body(not(containsString("at app.recipebook")));
    }

    @Test
    @DisplayName("AC-SVC-01 REVISION_CONFLICT の AppException を投げると 409 が返り、本文が code、message、details の形である")
    void appExceptionBecomesErrorResponse() {
        given().when().get("/api/probes/p-1/conflict")
                .then().statusCode(409)
                .contentType(ContentType.JSON)
                .body("code", equalTo("REVISION_CONFLICT"))
                .body("message", equalTo("ほかの人が先に保存しました。"))
                .body("details.currentRevision", equalTo(3))
                .body("size()", equalTo(3));
    }
}
