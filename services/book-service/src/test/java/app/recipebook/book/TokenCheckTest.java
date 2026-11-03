package app.recipebook.book;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import app.recipebook.common.testing.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 受け取ったトークンを、このサービスでも確かめていること(「共通設計: 認証」の 4.7)。
 * 確かめ方の場合分けは、service-common の TokenCheckTest で確かめている。
 * ここでは、このサービスの設定で、公開の鍵を取って確かめられることを見る。
 */
@QuarkusTest
class TokenCheckTest {

    private static final String SECURED = "/api/secured-probes";
    private static final UUID USER = UUID.fromString("0192f3a1-7c2e-7b4a-9d1e-3f5a6b7c8d9e");

    @Test
    @DisplayName("有効なトークンを付けて呼ぶと、200 が返り、本人の識別子を取り出せる")
    void validTokenIsAccepted() {
        given().header("Authorization", "Bearer " + TestTokens.forUser(USER))
                .when().get(SECURED).then().statusCode(200).body("userId", equalTo(USER.toString()));
    }

    @Test
    @DisplayName("トークンを付けずに呼ぶと、401 と UNAUTHENTICATED が返る")
    void missingTokenIsRejected() {
        given().when().get(SECURED).then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("AC-AUTHN-16 期限を 11 秒過ぎたトークンを付けてサービスを呼ぶと、401 が返る")
    void tokenExpiredElevenSecondsAgoIsRejected() {
        given().header("Authorization", "Bearer " + TestTokens.expiredSecondsAgo(USER, 11))
                .when().get(SECURED).then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("AC-AUTHN-17 別の鍵で署名したトークンを付けてサービスを呼ぶと、401 が返る")
    void tokenSignedWithOtherKeyIsRejected() {
        given().header("Authorization", "Bearer " + TestTokens.signedWithOtherKey(USER))
                .when().get(SECURED).then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
    }
}
