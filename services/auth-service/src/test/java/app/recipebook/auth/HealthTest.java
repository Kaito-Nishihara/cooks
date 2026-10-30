package app.recipebook.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import app.recipebook.auth.support.TestDatabase;
import io.quarkus.test.junit.QuarkusTest;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 稼働の確認(「共通設計: サービスの共通構成」の 4.8)。 */
@QuarkusTest
class HealthTest {

    private static final int MANAGEMENT_PORT = 9000;

    @Test
    @DisplayName("AC-SVC-14 ポート 9000 の /q/health/live、/q/health/ready、/q/health/started が 200 を返す")
    void healthIsServedOnManagementPort() {
        for (String path : new String[] {"/q/health/live", "/q/health/ready", "/q/health/started"}) {
            given().port(MANAGEMENT_PORT)
                    .when().get(path)
                    .then().statusCode(200).body("status", equalTo("UP"));
        }
    }

    @Test
    @DisplayName("AC-SVC-14 利用者の要求を受けるポートの同じパスは 404 を返す")
    void healthIsNotServedOnMainPort() {
        for (String path : new String[] {"/q/health/live", "/q/health/ready"}) {
            given().when().get(path)
                    .then().statusCode(404).body("code", equalTo("NOT_FOUND"));
        }
    }

    @Test
    @DisplayName("AC-SVC-15 データベースが接続を受け付けなくなると、/q/health/ready が 503 を返し、/q/health/live は 200 のままである")
    void readinessFollowsDatabase() {
        TestDatabase.refuseConnections("auth_test");
        try {
            given().port(MANAGEMENT_PORT).when().get("/q/health/ready").then().statusCode(503);
            given().port(MANAGEMENT_PORT).when().get("/q/health/live").then().statusCode(200);
        } finally {
            TestDatabase.acceptConnections("auth_test");
        }
        // 戻した後は、起動し直さなくても、また要求を受けられるようになる。切れた接続を捨てるまで、少しかかる
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                given().port(MANAGEMENT_PORT).when().get("/q/health/ready").then().statusCode(200));
    }
}
