package app.recipebook.auth.token;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 公開の鍵を返す入口(「共通設計: 認証」の 4.8)。 */
@QuarkusTest
class KeysApiTest {

    @Test
    @DisplayName("今の鍵と、入れ替えの間に残す古い鍵の 2 つを、JWKS の形で返す")
    void returnsCurrentAndRetiredKeys() {
        given().when().get("/internal/auth/keys").then().statusCode(200)
                .body("keys", hasSize(2))
                .body("keys.kty", everyItem(equalTo("RSA")))
                .body("keys.use", everyItem(equalTo("sig")))
                .body("keys.alg", everyItem(equalTo("RS256")))
                .body("keys.e", everyItem(equalTo("AQAB")));
    }

    @Test
    @DisplayName("公開の部分だけを返し、秘密の鍵の項目を含まない")
    void returnsOnlyPublicParts() {
        JsonPath body = given().when().get("/internal/auth/keys").then().statusCode(200).extract().jsonPath();
        List<Map<String, Object>> keys = body.getList("keys");

        for (Map<String, Object> key : keys) {
            assertThat(key).containsOnlyKeys("kty", "use", "alg", "kid", "n", "e");
            // 2,048 ビットの法は、URL に使える Base64 で 342 文字になる
            assertThat((String) key.get("n")).hasSize(342);
        }
        given().when().get("/internal/auth/keys").then()
                .body("keys", everyItem(not(hasKey("d"))));
    }

    @Test
    @DisplayName("鍵の名前は、鍵ごとに違い、URL に使える Base64 の 43 文字である")
    void keyIdsAreDistinct() {
        List<String> keyIds = given().when().get("/internal/auth/keys").then().extract().jsonPath().getList("keys.kid");

        assertThat(keyIds).doesNotHaveDuplicates().allMatch(kid -> kid.matches("[A-Za-z0-9_-]{43}"));
    }
}
