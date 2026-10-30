package app.recipebook.common;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

import app.recipebook.common.probe.RelayTargetResource;
import app.recipebook.common.support.LogFile;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 要求の識別子とログを確かめる(「共通設計: サービスの共通構成」の 4.5 と 4.6)。 */
@QuarkusTest
class RequestLogTest {

    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final String PROBE_ROUTE = "/api/probes/{probeId}";

    /** テストごとに重ならない、要求の識別子を作る。 */
    private static String newRequestId(String label) {
        return label + "-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("AC-SVC-02 X-Request-Id: abc-123 を付けて呼ぶと、応答とログの行の requestId が abc-123 である")
    void givenRequestIdIsUsed() {
        given().header("X-Request-Id", "abc-123")
                .when().get("/api/probes/p-1")
                .then().statusCode(200).header("X-Request-Id", equalTo("abc-123"));

        LogFile.awaitAccessRecord("abc-123", PROBE_ROUTE);
        assertThat(LogFile.linesOf("abc-123")).isNotEmpty();
    }

    @Test
    @DisplayName("AC-SVC-03 X-Request-Id を付けずに呼ぶと、応答の X-Request-Id が UUID の形である")
    void requestIdIsGeneratedWhenMissing() {
        given().when().get("/api/probes/p-1")
                .then().statusCode(200).header("X-Request-Id", matchesPattern(UUID_PATTERN));
    }

    @Test
    @DisplayName("AC-SVC-04 65 文字の値と、空白を含む値は使われず、応答の X-Request-Id が UUID の形である")
    void malformedRequestIdIsReplaced() {
        given().header("X-Request-Id", "a".repeat(65))
                .when().get("/api/probes/p-1")
                .then().statusCode(200).header("X-Request-Id", matchesPattern(UUID_PATTERN));

        given().header("X-Request-Id", "abc 123")
                .when().get("/api/probes/p-1")
                .then().statusCode(200).header("X-Request-Id", matchesPattern(UUID_PATTERN));
    }

    @Test
    @DisplayName("AC-SVC-04 64 文字の英数字とハイフンの値は、そのまま使われる")
    void longestRequestIdIsUsed() {
        String longest = "a".repeat(64);
        given().header("X-Request-Id", longest)
                .when().get("/api/probes/p-1")
                .then().statusCode(200).header("X-Request-Id", equalTo(longest));
    }

    @Test
    @DisplayName("エラーの応答にも X-Request-Id が付く")
    void errorResponseHasRequestId() {
        given().header("X-Request-Id", "err-404")
                .when().get("/api/no-such-path")
                .then().statusCode(404).header("X-Request-Id", equalTo("err-404"));
    }

    @Test
    @DisplayName("AC-SVC-05 ほかのサービスを呼ぶと、代役に届いた X-Request-Id と Authorization が元の要求と同じである")
    void headersArePropagatedToOtherService() {
        String requestId = newRequestId("relay");
        String authorization = "Bearer relay-token-" + UUID.randomUUID();

        given().header("X-Request-Id", requestId).header("Authorization", authorization)
                .when().get("/api/probes/p-1/relay")
                .then().statusCode(200).body("status", equalTo("ok"));

        assertThat(RelayTargetResource.lastHeaders())
                .containsEntry("X-Request-Id", requestId)
                .containsEntry("Authorization", authorization);
    }

    @Test
    @DisplayName("AC-SVC-06 要求を 1 つ処理すると、method、route、status、durationMs を持つログが 1 行だけ出る")
    void oneAccessRecordPerRequest() {
        String requestId = newRequestId("access");

        given().header("X-Request-Id", requestId)
                .when().get("/api/probes/secret-identifier-42?filter=query-value-77")
                .then().statusCode(200);

        LogFile.awaitAccessRecord(requestId, PROBE_ROUTE);
        List<JsonNode> records = LogFile.accessRecordsOf(requestId);
        assertThat(records).hasSize(1);

        JsonNode record = records.get(0);
        assertThat(record.path("level").asText()).isEqualTo("INFO");
        assertThat(record.path("service").asText()).isEqualTo("probe-service");
        assertThat(record.path("method").asText()).isEqualTo("GET");
        assertThat(record.path("route").asText()).isEqualTo(PROBE_ROUTE);
        assertThat(record.path("status").isInt()).isTrue();
        assertThat(record.path("status").asInt()).isEqualTo(200);
        assertThat(record.path("durationMs").isNumber()).isTrue();
        assertThat(record.path("timestamp").asText()).endsWith("Z");
        assertThat(record.has("errorCode")).isFalse();
        // パスの型を出す。実際の識別子と、クエリの変数の値を出さない
        assertThat(record.toString()).doesNotContain("secret-identifier-42").doesNotContain("query-value-77");
    }

    @Test
    @DisplayName("AC-SVC-06 エラーを返した要求の記録には、errorCode が入る")
    void accessRecordHasErrorCode() {
        String requestId = newRequestId("conflict");

        given().header("X-Request-Id", requestId)
                .when().get("/api/probes/p-1/conflict")
                .then().statusCode(409);

        String route = PROBE_ROUTE + "/conflict";
        LogFile.awaitAccessRecord(requestId, route);
        JsonNode record = LogFile.accessRecordsOf(requestId).get(0);
        assertThat(record.path("status").asInt()).isEqualTo(409);
        assertThat(record.path("errorCode").asText()).isEqualTo("REVISION_CONFLICT");
        // 利用者の誤りで返す 400 番台は、WARN にも ERROR にもしない
        assertThat(LogFile.linesOf(requestId)).allMatch(line -> "INFO".equals(line.path("level").asText()));
    }

    @Test
    @DisplayName("AC-SVC-07 パスワードとトークンを含む要求を処理した後、ログのどの行にも、その値が含まれない")
    void secretsAreNotLogged() {
        String password = "pw-" + UUID.randomUUID();
        String token = "token-" + UUID.randomUUID();
        String cookie = "cookie-" + UUID.randomUUID();
        String email = "mail-" + UUID.randomUUID() + "@example.com";
        String okId = newRequestId("secret-ok");
        String invalidId = newRequestId("secret-invalid");
        String crashId = newRequestId("secret-crash");

        // 成功する登録の要求
        given().header("X-Request-Id", okId)
                .header("Authorization", "Bearer " + token)
                .cookie("rb_session", cookie)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"n\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .when().post("/api/probes")
                .then().statusCode(201);
        // 検証で拒まれる要求
        given().header("X-Request-Id", invalidId)
                .header("Authorization", "Bearer " + token)
                .cookie("rb_session", cookie)
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"extra\":1}")
                .when().post("/api/probes")
                .then().statusCode(400);
        // 想定していない例外で終わる要求
        given().header("X-Request-Id", crashId)
                .header("Authorization", "Bearer " + token)
                .cookie("rb_session", cookie)
                .queryParam("email", email)
                .when().get("/api/probes/p-1/crash")
                .then().statusCode(500);

        LogFile.awaitAccessRecord(okId, "/api/probes");
        LogFile.awaitAccessRecord(invalidId, "/api/probes");
        LogFile.awaitAccessRecord(crashId, PROBE_ROUTE + "/crash");
        assertThat(LogFile.rawText())
                .doesNotContain(password)
                .doesNotContain(token)
                .doesNotContain(cookie)
                .doesNotContain(email);
    }

    @Test
    @DisplayName("AC-SVC-08 想定していない例外が起きると、ERROR の行が 1 つ出て、スタックトレースと requestId を持つ")
    void unexpectedExceptionIsLoggedOnce() {
        String requestId = newRequestId("crash");

        given().header("X-Request-Id", requestId)
                .when().get("/api/probes/p-1/crash")
                .then().statusCode(500);

        LogFile.awaitAccessRecord(requestId, PROBE_ROUTE + "/crash");
        List<JsonNode> errors = LogFile.linesOf(requestId).stream()
                .filter(line -> "ERROR".equals(line.path("level").asText()))
                .toList();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).path("stackTrace").asText())
                .contains("IllegalStateException")
                .contains("ProbeResource.crash");
    }

    @Test
    @DisplayName("ないパスの要求の記録は、route が null で、実際のパスを含まない")
    void unmatchedPathIsNotLogged() {
        String requestId = newRequestId("unmatched");

        given().header("X-Request-Id", requestId)
                .when().get("/api/no-such-path/hidden-segment-9")
                .then().statusCode(404);

        org.awaitility.Awaitility.await().until(() -> !LogFile.accessRecordsOf(requestId).isEmpty());
        JsonNode record = LogFile.accessRecordsOf(requestId).get(0);
        assertThat(record.path("route").isNull()).isTrue();
        assertThat(record.path("errorCode").asText()).isEqualTo("NOT_FOUND");
        assertThat(record.toString()).doesNotContain("hidden-segment-9");
    }
}
