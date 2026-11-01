package app.recipebook.auth.support;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.LinkedHashMap;
import java.util.Map;

/** テストから、登録とログインの API を呼ぶための道具。利用者を作る処理を、ここ 1 か所に置く。 */
public final class AuthApi {

    /** テストの環境の、画面のオリジン */
    public static final String ORIGIN = "http://localhost:8080";
    public static final String COOKIE = "rb_session";

    public static final String NAME = "みさき";
    public static final String EMAIL = "misaki@example.com";
    public static final String PASSWORD = "ながいあいことばをつかう2026";

    private AuthApi() {
    }

    /** 自分のサイトの画面から送る、JSON の要求。 */
    public static RequestSpecification fromOwnSite() {
        return given().header("Origin", ORIGIN).contentType(ContentType.JSON.withCharset("UTF-8"));
    }

    public static Response register(String displayName, String email, String password) {
        return fromOwnSite().body(body("displayName", displayName, "email", email, "password", password))
                .when().post("/api/auth/register");
    }

    /** 正しい入力で登録する。 */
    public static Response registerMisaki() {
        return register(NAME, EMAIL, PASSWORD);
    }

    public static Response login(String email, String password) {
        return fromOwnSite().body(body("email", email, "password", password))
                .when().post("/api/auth/login");
    }

    /** 入口の代わりに verify を呼び、このクッキーのセッションのトークンを取る。 */
    public static String tokenFor(String cookie) {
        String authorization = given().cookie(COOKIE, cookie).get("/internal/auth/verify/api/me")
                .then().statusCode(200).extract().header("Authorization");
        return authorization.substring("Bearer ".length());
    }

    /** トークンとクッキーを付けた要求。入口を通った要求の代わりに使う。 */
    public static RequestSpecification signedIn(String cookie) {
        return given().header("Authorization", "Bearer " + tokenFor(cookie)).cookie(COOKIE, cookie);
    }

    /** 名前と値を交互に渡して、本文を作る。値が null の項目は、入れない。 */
    public static Map<String, Object> body(Object... namesAndValues) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            if (namesAndValues[i + 1] != null) {
                body.put((String) namesAndValues[i], namesAndValues[i + 1]);
            }
        }
        return body;
    }
}
