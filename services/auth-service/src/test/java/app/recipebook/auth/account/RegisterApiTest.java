package app.recipebook.auth.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

import app.recipebook.auth.support.AuthApi;
import app.recipebook.auth.support.MutableClock;
import app.recipebook.auth.support.TestDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 登録の API(「機能設計: 登録とログイン」の 4.3)。 */
@QuarkusTest
class RegisterApiTest {

    @Inject
    MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.reset();
        TestDatabase.truncateAll();
    }

    @Test
    @DisplayName("AC-AUTH-09 正しい入力で登録すると、201 が返り、本文が id、email、displayName の 3 つだけで、Set-Cookie に rb_session がある")
    void registerReturnsUserAndCookie() {
        Response response = AuthApi.registerMisaki();

        response.then().statusCode(201)
                .body("size()", equalTo(3))
                .body("id", matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"))
                .body("email", equalTo("misaki@example.com"))
                .body("displayName", equalTo("みさき"));
        assertThat(response.getCookie(AuthApi.COOKIE)).hasSize(43);
        assertThat(response.getHeader("Location")).isNull();
    }

    @Test
    @DisplayName("AC-AUTHN-22 クッキーなしで POST /api/auth/register を呼べる")
    void registerNeedsNoCookie() {
        AuthApi.registerMisaki().then().statusCode(201);
    }

    @Test
    @DisplayName("AC-AUTHN-02 users のパスワードのハッシュが $2 で始まる 60 文字で、登録したパスワードそのものではない")
    void passwordIsStoredAsHash() {
        AuthApi.registerMisaki().then().statusCode(201);

        String hash = TestDatabase.queryStrings("SELECT password_hash FROM users").get(0);
        assertThat(hash).hasSize(60).startsWith("$2").isNotEqualTo(AuthApi.PASSWORD);
    }

    @Test
    @DisplayName("AC-AUTH-03 登録済みのメールアドレスで登録すると、409 と EMAIL_TAKEN が返り、users の行が増えない")
    void duplicateEmailIsRejected() {
        AuthApi.registerMisaki().then().statusCode(201);

        AuthApi.register("べつのひと", AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(409)
                .body("code", equalTo("EMAIL_TAKEN"))
                .body("message", equalTo("このメールアドレスは、すでに登録されています。"));
        assertThat(TestDatabase.count("users")).isEqualTo(1);
        assertThat(TestDatabase.count("sessions")).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-AUTH-04 パスワードを半角の英数字 11 文字にすると、400 が返り、fields に password と TOO_SHORT の 1 件が入る")
    void shortPasswordIsRejected() {
        AuthApi.register(AuthApi.NAME, AuthApi.EMAIL, "abcdefghi12").then().statusCode(400)
                .body("code", equalTo("VALIDATION_FAILED"))
                .body("details.fields", hasSize(1))
                .body("details.fields[0].path", equalTo("password"))
                .body("details.fields[0].code", equalTo("TOO_SHORT"))
                .body("details.fields[0].message", equalTo("12 文字以上で入れてください。"));
        assertThat(TestDatabase.count("users")).isZero();
    }

    @Test
    @DisplayName("AC-AUTHN-04 UTF-8 で 73 バイトのパスワードで登録すると、400 と VALIDATION_FAILED が返る")
    void longPasswordIsRejected() {
        AuthApi.register(AuthApi.NAME, AuthApi.EMAIL, "a".repeat(73)).then().statusCode(400)
                .body("code", equalTo("VALIDATION_FAILED"))
                .body("details.fields[0].path", equalTo("password"))
                .body("details.fields[0].code", equalTo("TOO_LONG"));
        // ひらがな 25 文字は 75 バイトである
        AuthApi.register(AuthApi.NAME, AuthApi.EMAIL, "あ".repeat(25)).then().statusCode(400)
                .body("details.fields[0].code", equalTo("TOO_LONG"));
        // 72 バイトちょうどは通る
        AuthApi.register(AuthApi.NAME, AuthApi.EMAIL, "a".repeat(72)).then().statusCode(201);
    }

    @Test
    @DisplayName("AC-AUTH-11 メールアドレスの前後の空白を取り、小文字にそろえて登録する")
    void emailIsNormalized() {
        AuthApi.register(AuthApi.NAME, "  Misaki@Example.COM ", AuthApi.PASSWORD).then().statusCode(201)
                .body("email", equalTo("misaki@example.com"));
        assertThat(TestDatabase.queryStrings("SELECT email FROM users")).containsExactly("misaki@example.com");
    }

    @Test
    @DisplayName("AC-AUTH-12 大文字と小文字だけが違うメールアドレスで登録すると、409 と EMAIL_TAKEN が返る")
    void emailIsComparedIgnoringCase() {
        AuthApi.registerMisaki().then().statusCode(201);

        AuthApi.register(AuthApi.NAME, "MISAKI@example.com", AuthApi.PASSWORD).then().statusCode(409)
                .body("code", equalTo("EMAIL_TAKEN"));
    }

    @Test
    @DisplayName("AC-AUTH-13 「+」より後ろが違うメールアドレスは、別のものとして登録できる")
    void plusAddressIsAnotherEmail() {
        AuthApi.registerMisaki().then().statusCode(201);

        AuthApi.register(AuthApi.NAME, "misaki+a@example.com", AuthApi.PASSWORD).then().statusCode(201)
                .body("email", equalTo("misaki+a@example.com"));
    }

    @Test
    @DisplayName("AC-AUTH-14 形が合わないメールアドレスで登録すると、400 が返り、fields に email と INVALID_FORMAT の 1 件が入る")
    void malformedEmailIsRejected() {
        List<String> malformed = List.of(
                "misaki@localhost",
                "misaki@example..com",
                "みさき@example.com",
                "ｍｉｓａｋｉ＠ｅｘａｍｐｌｅ．ｃｏｍ",
                "misaki example@example.com");
        for (String email : malformed) {
            AuthApi.register(AuthApi.NAME, email, AuthApi.PASSWORD).then().statusCode(400)
                    .body("details.fields", hasSize(1))
                    .body("details.fields[0].path", equalTo("email"))
                    .body("details.fields[0].code", equalTo("INVALID_FORMAT"))
                    .body("details.fields[0].message",
                            equalTo("メールアドレスの形が正しくありません。半角の英数字と記号で入れてください。"));
        }
        assertThat(TestDatabase.count("users")).isZero();
    }

    @Test
    @DisplayName("AC-AUTH-15 表示名の前後の全角の空白を取って登録する")
    void displayNameIsTrimmed() {
        AuthApi.register("　みさき　", AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(201)
                .body("displayName", equalTo("みさき"));
    }

    @Test
    @DisplayName("AC-AUTH-16 表示名を 50 文字にして登録すると、201 が返る")
    void displayNameOfFiftyCharactersIsAccepted() {
        AuthApi.register("あ".repeat(50), AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(201);
    }

    @Test
    @DisplayName("AC-AUTH-17 表示名を 51 文字にして登録すると、400 が返り、fields に displayName と TOO_LONG の 1 件が入る")
    void displayNameOfFiftyOneCharactersIsRejected() {
        AuthApi.register("あ".repeat(51), AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(400)
                .body("details.fields", hasSize(1))
                .body("details.fields[0].path", equalTo("displayName"))
                .body("details.fields[0].code", equalTo("TOO_LONG"))
                .body("details.fields[0].message", equalTo("50 文字までで入れてください。"));
    }

    @Test
    @DisplayName("AC-AUTH-18 表示名を「𠮷」50 個にして登録すると、201 が返る")
    void displayNameIsCountedByCodePoint() {
        String name = "𠮷".repeat(50);
        AuthApi.register(name, AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(201)
                .body("displayName", equalTo(name));
    }

    @Test
    @DisplayName("AC-AUTH-19 表示名を全角の空白 3 つにして登録すると、400 が返り、fields に displayName と REQUIRED の 1 件が入る")
    void blankDisplayNameIsRejected() {
        AuthApi.register("　　　", AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(400)
                .body("details.fields", hasSize(1))
                .body("details.fields[0].path", equalTo("displayName"))
                .body("details.fields[0].code", equalTo("REQUIRED"))
                .body("details.fields[0].message", equalTo("表示名を入れてください。"));
    }

    @Test
    @DisplayName("AC-AUTH-20 表示名の途中に改行を入れて登録すると、400 が返り、fields に displayName と INVALID_FORMAT の 1 件が入る")
    void displayNameWithLineBreakIsRejected() {
        AuthApi.register("みさ\nき", AuthApi.EMAIL, AuthApi.PASSWORD).then().statusCode(400)
                .body("details.fields", hasSize(1))
                .body("details.fields[0].path", equalTo("displayName"))
                .body("details.fields[0].code", equalTo("INVALID_FORMAT"));
    }

    @Test
    @DisplayName("AC-AUTH-21 本文を {} にして登録すると、400 が返り、fields に 3 件が、どれも REQUIRED で入る")
    void emptyBodyReturnsAllRequired() {
        AuthApi.fromOwnSite().body("{}").when().post("/api/auth/register").then().statusCode(400)
                .body("details.fields", hasSize(3))
                .body("details.fields.path", containsInAnyOrder("displayName", "email", "password"))
                .body("details.fields.code", everyItem(equalTo("REQUIRED")));
    }

    @Test
    @DisplayName("文字列でない値と、知らない項目は、400 で拒む")
    void wrongTypeAndUnknownFieldAreRejected() {
        AuthApi.fromOwnSite().body(AuthApi.body("displayName", 123, "email", AuthApi.EMAIL, "password", AuthApi.PASSWORD))
                .when().post("/api/auth/register").then().statusCode(400)
                .body("details.fields[0].path", equalTo("displayName"))
                .body("details.fields[0].code", equalTo("INVALID_FORMAT"));
        AuthApi.fromOwnSite().body(AuthApi.body("displayName", AuthApi.NAME, "email", AuthApi.EMAIL,
                        "password", AuthApi.PASSWORD, "role", "owner"))
                .when().post("/api/auth/register").then().statusCode(400)
                .body("details.fields[0].path", equalTo("role"))
                .body("details.fields[0].code", equalTo("UNKNOWN_FIELD"));
        assertThat(TestDatabase.count("users")).isZero();
    }

    @Test
    @DisplayName("AC-AUTH-22 パスワードをひらがな 12 文字にして登録すると、201 が返る")
    void hiraganaPasswordIsAccepted() {
        AuthApi.register(AuthApi.NAME, AuthApi.EMAIL, "あ".repeat(12)).then().statusCode(201);
    }

    @Test
    @DisplayName("AC-AUTH-25 同じメールアドレスの登録を 2 つ同時に送ると、片方に 201、もう片方に 409 が返り、users の行は 1 つである")
    void concurrentRegistrationCreatesOneUser() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> register = () -> {
            start.await();
            return AuthApi.registerMisaki().statusCode();
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(register);
            Future<Integer> second = executor.submit(register);
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        } finally {
            executor.shutdownNow();
        }
        assertThat(TestDatabase.count("users")).isEqualTo(1);
        assertThat(TestDatabase.count("sessions")).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-AUTH-26 Origin をほかのサイトにして登録すると、403 と CROSS_SITE_REQUEST が返り、users に行ができない")
    void crossSiteRegistrationIsRejected() {
        var body = AuthApi.body("displayName", AuthApi.NAME, "email", AuthApi.EMAIL, "password", AuthApi.PASSWORD);

        RestAssured.given().header("Origin", "https://evil.example").contentType(ContentType.JSON.withCharset("UTF-8"))
                .body(body).when().post("/api/auth/register")
                .then().statusCode(403).body("code", equalTo("CROSS_SITE_REQUEST"));
        // Origin がないときも拒む
        RestAssured.given().contentType(ContentType.JSON.withCharset("UTF-8"))
                .body(body).when().post("/api/auth/register")
                .then().statusCode(403).body("code", equalTo("CROSS_SITE_REQUEST"));
        // 入力の誤りより先に、送り元を確かめる
        RestAssured.given().header("Origin", "https://evil.example").contentType(ContentType.JSON)
                .body("{}").when().post("/api/auth/register")
                .then().statusCode(403);
        assertThat(TestDatabase.count("users")).isZero();
    }

    @Test
    @DisplayName("AC-AUTH-27 利用者 A のクッキーを付けて利用者 B を登録すると、201 が返り、A のセッションがなくなる(セッションの行で確かめる)")
    void registeringWithCookieRemovesPreviousSession() {
        String cookieOfA = AuthApi.registerMisaki().getCookie(AuthApi.COOKIE);
        String userIdOfA = TestDatabase.queryStrings("SELECT id::text FROM users").get(0);

        AuthApi.fromOwnSite().cookie(AuthApi.COOKIE, cookieOfA)
                .body(AuthApi.body("displayName", "おかあさん", "email", "haha@example.com", "password", AuthApi.PASSWORD))
                .when().post("/api/auth/register")
                .then().statusCode(201).cookie(AuthApi.COOKIE, notNullValue());

        assertThat(TestDatabase.count("sessions")).isEqualTo(1);
        assertThat(TestDatabase.queryStrings("SELECT user_id::text FROM sessions")).doesNotContain(userIdOfA);
    }
}
