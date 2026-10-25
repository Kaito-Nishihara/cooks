package app.recipebook.auth;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 書き出す OpenAPI の仕様(「共通設計: API の共通仕様」の 4.11)。 */
@QuarkusTest
class OpenApiDocumentTest {

    private static final int MANAGEMENT_PORT = 9000;

    /** 画面に出す API の仕様 */
    private static JsonPath publicDocument() {
        return fetch("/q/openapi");
    }

    /** サービスの間だけで使う API の仕様 */
    private static JsonPath internalDocument() {
        return fetch("/q/openapi-internal");
    }

    private static JsonPath fetch(String path) {
        return given().port(MANAGEMENT_PORT).queryParam("format", "json")
                .when().get(path)
                .then().statusCode(200).extract().jsonPath();
    }

    @Test
    @DisplayName("画面に出す仕様は /api のパスだけを持ち、/internal を持たない")
    void publicDocumentHasOnlyApiPaths() {
        Map<String, Object> paths = publicDocument().getMap("paths");

        assertThat(paths.keySet())
                .contains("/api/auth/register", "/api/auth/login", "/api/auth/logout", "/api/me", "/api/me/password")
                .allMatch(path -> path.startsWith("/api/"));
    }

    @Test
    @DisplayName("サービスの間の仕様は /internal のパスだけを持つ")
    void internalDocumentHasOnlyInternalPaths() {
        Map<String, Object> paths = internalDocument().getMap("paths");

        assertThat(paths.keySet())
                .contains("/internal/users", "/internal/auth/keys")
                .allMatch(path -> path.startsWith("/internal/"));
    }

    @Test
    @DisplayName("すべての操作に、camelCase の operationId が重ならずに付いている")
    void everyOperationHasCamelCaseId() {
        for (JsonPath document : List.of(publicDocument(), internalDocument())) {
            List<String> ids = operationIds(document);

            assertThat(ids).isNotEmpty()
                    .allMatch(id -> id != null && id.matches("[a-z]+([A-Z][a-z0-9]*)+"), "動詞と名詞の camelCase")
                    .doesNotHaveDuplicates();
        }
    }

    @Test
    @DisplayName("画面が使う操作の名前が、決めたとおりである")
    void publicOperationIds() {
        assertThat(operationIds(publicDocument()))
                .containsExactlyInAnyOrder("registerUser", "loginUser", "logoutUser", "getCurrentUser", "changePassword");
    }

    @Test
    @DisplayName("どちらの仕様にも、エラーの形 ErrorResponse があり、すべての操作のエラーの応答がそれを指す")
    void errorResponseIsShared() {
        for (JsonPath document : List.of(publicDocument(), internalDocument())) {
            assertThat(document.getMap("components.schemas.ErrorResponse.properties").keySet())
                    .containsExactlyInAnyOrder("code", "message", "details");

            Map<String, Map<String, Object>> paths = document.getMap("paths");
            paths.forEach((path, operations) -> operations.keySet().forEach(method ->
                    assertThat(document.getString(
                            "paths.'%s'.%s.responses.default.content.'application/json'.schema.$ref".formatted(path, method)))
                            .as("%s %s", method, path)
                            .isEqualTo("#/components/schemas/ErrorResponse")));
        }
    }

    @Test
    @DisplayName("入力の長さの決まりが、仕様に出ている")
    void inputConstraintsAreDescribed() {
        JsonPath document = publicDocument();

        assertThat(document.getInt("components.schemas.RegisterRequest.properties.displayName.maxLength")).isEqualTo(50);
        assertThat(document.getInt("components.schemas.RegisterRequest.properties.email.maxLength")).isEqualTo(254);
        assertThat(document.getInt("components.schemas.RegisterRequest.properties.password.minLength")).isEqualTo(12);
        assertThat(document.getList("components.schemas.RegisterRequest.required", String.class))
                .containsExactlyInAnyOrder("displayName", "email", "password");
    }

    private static List<String> operationIds(JsonPath document) {
        List<String> ids = new ArrayList<>();
        Map<String, Map<String, Map<String, Object>>> paths = document.getMap("paths");
        paths.values().forEach(operations -> operations.values().forEach(op -> ids.add((String) op.get("operationId"))));
        return ids;
    }
}
