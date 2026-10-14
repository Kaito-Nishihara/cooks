package app.recipebook.common;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import app.recipebook.common.support.LogFile;
import app.recipebook.common.testing.TestTokens;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 受け取ったトークンの確かめ方(「共通設計: 認証」の 4.7)。 */
@QuarkusTest
class TokenCheckTest {

    private static final String SECURED = "/api/secured-probes";
    private static final UUID USER = UUID.fromString("0192f3a1-7c2e-7b4a-9d1e-3f5a6b7c8d9e");

    private static void expectUnauthenticated(String authorization) {
        var request = given();
        if (authorization != null) {
            request = request.header("Authorization", authorization);
        }
        request.when().get(SECURED).then().statusCode(401)
                .body("code", equalTo("UNAUTHENTICATED"))
                .body("message", equalTo("ログインが必要です。"))
                .body("details", nullValue());
    }

    @Test
    @DisplayName("有効なトークンを付けて呼ぶと、200 が返り、本人の識別子を取り出せる")
    void validTokenIsAccepted() {
        given().header("Authorization", "Bearer " + TestTokens.forUser(USER))
                .when().get(SECURED).then().statusCode(200).body("userId", equalTo(USER.toString()));
    }

    @Test
    @DisplayName("AC-AUTHN-16 期限を 11 秒過ぎたトークンを付けてサービスを呼ぶと、401 が返る")
    void tokenExpiredElevenSecondsAgoIsRejected() {
        expectUnauthenticated("Bearer " + TestTokens.expiredSecondsAgo(USER, 11));
    }

    @Test
    @DisplayName("期限を 9 秒過ぎたトークンは、時計のずれとして受け付ける")
    void tokenWithinGraceIsAccepted() {
        given().header("Authorization", "Bearer " + TestTokens.expiredSecondsAgo(USER, 9))
                .when().get(SECURED).then().statusCode(200);
    }

    @Test
    @DisplayName("AC-AUTHN-17 別の鍵で署名したトークンを付けてサービスを呼ぶと、401 が返る")
    void tokenSignedWithOtherKeyIsRejected() {
        expectUnauthenticated("Bearer " + TestTokens.signedWithOtherKey(USER));
    }

    @Test
    @DisplayName("トークンがない、形が壊れている、発行者や使う場所が違う、sub が UUID でないときは、401 が返る")
    void malformedTokensAreRejected() {
        expectUnauthenticated(null);
        expectUnauthenticated("Bearer not-a-token");
        expectUnauthenticated("Bearer " + TestTokens.withClaims(USER.toString(), "someone-else", "recipe-book"));
        expectUnauthenticated("Bearer " + TestTokens.withClaims(USER.toString(), "auth-service", "other-app"));
        expectUnauthenticated("Bearer " + TestTokens.withClaims("misaki", "auth-service", "recipe-book"));
    }

    @Test
    @DisplayName("PublicEndpoint の印がある入口は、トークンなしで呼べ、付いていても確かめない")
    void publicEndpointNeedsNoToken() {
        given().when().get("/api/probes/p-1").then().statusCode(200);
        // 壊れたトークンが付いていても、確かめずに通す
        given().header("Authorization", "Bearer not-a-token").when().get("/api/probes/p-1").then().statusCode(200);
    }

    @Test
    @DisplayName("本人を確かめた要求のログの行に、userId が付き、トークンは出ない")
    void userIdIsLoggedWithoutToken() {
        String requestId = "token-log-" + UUID.randomUUID();
        String token = TestTokens.forUser(USER);

        given().header("X-Request-Id", requestId).header("Authorization", "Bearer " + token)
                .when().get(SECURED).then().statusCode(200);

        LogFile.awaitAccessRecord(requestId, SECURED);
        JsonNode record = LogFile.accessRecordsOf(requestId).get(0);
        assertThat(record.path("userId").asText()).isEqualTo(USER.toString());
        assertThat(LogFile.rawText()).doesNotContain(token);
    }
}
