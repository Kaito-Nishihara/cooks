package app.recipebook.auth.account;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import app.recipebook.auth.support.AuthApi;
import app.recipebook.auth.support.MutableClock;
import app.recipebook.auth.support.TestDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 表示名をまとめて返す内部の API(「機能設計: 登録とログイン」の 4.8)。 */
@QuarkusTest
class InternalUsersApiTest {

    /** どの利用者のものでもない識別子 */
    private static final String UNKNOWN_ID = "0192f3a2-11aa-7c3d-8e2f-4a5b6c7d8e9f";

    @Inject
    MutableClock clock;

    private String misakiId;
    private String token;

    @BeforeEach
    void setUp() {
        clock.resetToNow();
        TestDatabase.truncateAll();
        var registered = AuthApi.registerMisaki();
        misakiId = registered.jsonPath().getString("id");
        token = AuthApi.tokenFor(registered.getCookie(AuthApi.COOKIE));
    }

    private RequestSpecification asService() {
        return given().header("Authorization", "Bearer " + token);
    }

    @Test
    @DisplayName("AC-AUTH-47 登録済みの 1 人と登録のない 1 件を並べると、items が 1 件で、項目が id と displayName だけで、nextCursor が null である")
    void listsOnlyRegisteredUsers() {
        JsonPath body = asService().queryParam("ids", misakiId + "," + UNKNOWN_ID).get("/internal/users")
                .then().statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].id", equalTo(misakiId))
                .body("items[0].displayName", equalTo(AuthApi.NAME))
                .body("nextCursor", nullValue())
                .extract().jsonPath();

        Map<String, Object> item = body.getMap("items[0]");
        assertThat(item).containsOnlyKeys("id", "displayName");
        assertThat(body.getMap("$")).containsOnlyKeys("items", "nextCursor");
    }

    @Test
    @DisplayName("見つかった利用者を識別子の昇順に並べ、同じ識別子を 2 回渡されても 1 件だけ返す")
    void sortsByIdAndRemovesDuplicates() {
        String otherId = AuthApi.register("おかあさん", "mother@example.com", AuthApi.PASSWORD).jsonPath().getString("id");
        String first = misakiId.compareTo(otherId) < 0 ? misakiId : otherId;
        String second = first.equals(misakiId) ? otherId : misakiId;

        asService().queryParam("ids", second + "," + first + "," + second).get("/internal/users")
                .then().statusCode(200)
                .body("items.id", contains(first, second));
    }

    @Test
    @DisplayName("AC-AUTH-48 ids を付けないと、400 と、fields の code が REQUIRED で返る")
    void idsIsRequired() {
        asService().get("/internal/users")
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_FAILED"))
                .body("details.fields", hasSize(1))
                .body("details.fields[0].path", equalTo("ids"))
                .body("details.fields[0].code", equalTo("REQUIRED"));
    }

    @Test
    @DisplayName("AC-AUTH-48 ids を 101 件にすると、400 と、fields の code が TOO_MANY で返る")
    void atMostOneHundredIds() {
        String ids = IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.joining(","));

        asService().queryParam("ids", ids).get("/internal/users")
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_FAILED"))
                .body("details.fields[0].path", equalTo("ids"))
                .body("details.fields[0].code", equalTo("TOO_MANY"));
    }

    @Test
    @DisplayName("ids がちょうど 100 件なら、200 が返る")
    void oneHundredIdsAreAccepted() {
        String ids = IntStream.range(0, 99).mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.joining(",", misakiId + ",", ""));

        asService().queryParam("ids", ids).get("/internal/users")
                .then().statusCode(200)
                .body("items", hasSize(1));
    }

    @Test
    @DisplayName("AC-AUTH-48 ids に abc を混ぜると、400 と、fields の code が INVALID_FORMAT で返る")
    void idsMustBeUuids() {
        asService().queryParam("ids", misakiId + ",abc").get("/internal/users")
                .then().statusCode(400)
                .body("code", equalTo("VALIDATION_FAILED"))
                .body("details.fields[0].path", equalTo("ids"))
                .body("details.fields[0].code", equalTo("INVALID_FORMAT"));
    }

    @Test
    @DisplayName("AC-AUTH-49 トークンを付けずに呼ぶと、401 と UNAUTHENTICATED が返る")
    void tokenIsRequired() {
        given().queryParam("ids", misakiId).get("/internal/users")
                .then().statusCode(401)
                .body("code", equalTo("UNAUTHENTICATED"));
    }
}
