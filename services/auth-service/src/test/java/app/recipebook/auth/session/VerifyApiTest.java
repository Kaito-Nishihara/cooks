package app.recipebook.auth.session;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import app.recipebook.auth.support.AuthApi;
import app.recipebook.auth.support.MutableClock;
import app.recipebook.auth.support.TestDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 入口から呼ばれる verify(「共通設計: 認証」の 4.6)。 */
@QuarkusTest
class VerifyApiTest {

    private static final String OTHER_SITE = "https://evil.example";

    @Inject
    MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.reset();
        TestDatabase.truncateAll();
    }

    private static Response verify(String method, String cookie) {
        var request = given().header("Origin", AuthApi.ORIGIN);
        if (cookie != null) {
            request = request.cookie(AuthApi.COOKIE, cookie);
        }
        return request.request(method, "/internal/auth/verify/api/books/0192f3a1-7c2e-7b4a-9d1e-3f5a6b7c8d9e");
    }

    private static String lastUsedAndExpiry() {
        return TestDatabase.queryStrings("SELECT last_used_at::text || ' ' || expires_at::text FROM sessions").get(0);
    }

    @Test
    @DisplayName("有効なセッションのクッキーで呼ぶと、200 と Authorization: Bearer のトークンが返る")
    void validSessionGetsToken() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);

        for (String method : List.of("GET", "POST", "PUT", "PATCH", "DELETE")) {
            verify(method, cookie).then().statusCode(200).header("Authorization", startsWith("Bearer ey"));
        }
    }

    @Test
    @DisplayName("AC-AUTHN-15 verify が返すトークンの exp と iat の差が 60 で、sub が利用者の識別子で、メールアドレスと表示名が含まれない")
    void tokenFromVerifyHasRequiredClaims() {
        Response registered = AuthApi.registerMisaki();

        String token = verify("GET", registered.getCookie(AuthApi.COOKIE)).getHeader("Authorization").substring(7);
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        var claims = io.restassured.path.json.JsonPath.from(payload);

        assertThat(claims.getLong("exp") - claims.getLong("iat")).isEqualTo(60);
        assertThat(claims.getString("sub")).isEqualTo(registered.jsonPath().getString("id"));
        assertThat(payload).doesNotContain(AuthApi.EMAIL).doesNotContain(AuthApi.NAME);
    }

    @Test
    @DisplayName("クッキーがない、形が合わない、どのセッションも指さないときは、401 と UNAUTHENTICATED が返る")
    void missingSessionIsUnauthenticated() {
        verify("GET", null).then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
        verify("GET", "こわれた値").then().statusCode(401);
        verify("GET", "A".repeat(43)).then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("AC-AUTHN-09 期限が切れたセッションのクッキーで verify を呼ぶと、401 と UNAUTHENTICATED が返る")
    void expiredSessionIsUnauthenticated() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);

        clock.advance(Duration.ofDays(30).minusSeconds(1));
        verify("GET", cookie).then().statusCode(200);

        // 使ったので、最後に使った瞬間から 30 日後まで延びている。その時刻を過ぎると切れる
        clock.advance(Duration.ofDays(30));
        verify("GET", cookie).then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("AC-AUTHN-10 最後に使った日時が 2 日前のセッションで verify を呼ぶと、最後に使った日時と期限が今の時刻をもとに書き換わる")
    void usageIsRecordedAfterTwoDays() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        assertThat(lastUsedAndExpiry()).isEqualTo("2026-10-10 03:00:00+00 2026-11-09 03:00:00+00");

        clock.advance(Duration.ofDays(2));
        verify("GET", cookie).then().statusCode(200);

        assertThat(lastUsedAndExpiry()).isEqualTo("2026-10-12 03:00:00+00 2026-11-11 03:00:00+00");
    }

    @Test
    @DisplayName("最後に使った日時が 1 日より新しいときは、記録を書き換えない")
    void usageIsNotRecordedWithinADay() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);

        clock.advance(Duration.ofHours(23));
        verify("GET", cookie).then().statusCode(200);

        assertThat(lastUsedAndExpiry()).isEqualTo("2026-10-10 03:00:00+00 2026-11-09 03:00:00+00");
    }

    @Test
    @DisplayName("AC-AUTHN-11 作ってから 91 日たったセッションは、前の日に使っていても、verify が 401 を返す")
    void sessionEndsNinetyDaysAfterCreation() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);

        for (int day = 1; day <= 90; day++) {
            clock.advance(Duration.ofDays(1));
            if (day < 90) {
                verify("GET", cookie).then().statusCode(200);
            }
        }
        // 90 日目の前の日(89 日目)に使っていても、90 日で切れる
        verify("GET", cookie).then().statusCode(401);
        clock.advance(Duration.ofDays(1));
        verify("GET", cookie).then().statusCode(401).body("code", equalTo("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("AC-AUTHN-12 11 回ログインすると、最初のセッションのクッキーで verify が 401 を返し、残りの 10 個は 200 を返す")
    void eleventhLoginEvictsOldest() {
        List<String> cookies = new ArrayList<>();
        // 登録で 1 個、ログインで 10 個、合わせて 11 個のセッションを作る
        cookies.add(AuthApi.registerMisaki().getCookie(AuthApi.COOKIE));
        for (int i = 0; i < 10; i++) {
            clock.advance(Duration.ofSeconds(1));
            cookies.add(AuthApi.login(AuthApi.EMAIL, AuthApi.PASSWORD).getCookie(AuthApi.COOKIE));
        }

        verify("GET", cookies.get(0)).then().statusCode(401);
        for (String cookie : cookies.subList(1, 11)) {
            verify("GET", cookie).then().statusCode(200);
        }
    }

    @Test
    @DisplayName("AC-AUTHN-13 Origin がほかのサイトの POST で verify を呼ぶと 403 と CROSS_SITE_REQUEST が返り、GET では 200 が返る")
    void crossSiteStateChangingRequestIsRejected() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        String path = "/internal/auth/verify/api/books/0192f3a1-7c2e-7b4a-9d1e-3f5a6b7c8d9e";

        given().header("Origin", OTHER_SITE).cookie(AuthApi.COOKIE, cookie).post(path)
                .then().statusCode(403).body("code", equalTo("CROSS_SITE_REQUEST"));
        given().header("Origin", OTHER_SITE).cookie(AuthApi.COOKIE, cookie).get(path)
                .then().statusCode(200);
        // 状態を変える要求で Origin がないときも拒む
        given().cookie(AuthApi.COOKIE, cookie).delete(path)
                .then().statusCode(403).body("code", equalTo("CROSS_SITE_REQUEST"));
        // 状態を変えない要求では、Origin がなくても通す
        given().cookie(AuthApi.COOKIE, cookie).get(path).then().statusCode(200);
    }

    @Test
    @DisplayName("後ろに付いたパスは使わない。パスがなくても、同じように答える")
    void originalPathIsIgnored() {
        String cookie = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);

        given().cookie(AuthApi.COOKIE, cookie).get("/internal/auth/verify").then().statusCode(200);
        given().cookie(AuthApi.COOKIE, cookie).get("/internal/auth/verify/api/me").then().statusCode(200);
    }
}
