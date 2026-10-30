package app.recipebook.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import app.recipebook.auth.shared.domain.UserId;
import app.recipebook.auth.token.application.AccessTokenIssuer;
import app.recipebook.common.core.UuidV7;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** トークンの中身と署名(「共通設計: 認証」の 4.7)。 */
@QuarkusTest
class AccessTokenIssuerTest {

    private static final Instant NOW = Instant.parse("2026-10-10T03:00:00Z");

    @Inject
    AccessTokenIssuer issuer;

    @Test
    @DisplayName("AC-AUTHN-15 トークンの exp と iat の差が 60 で、sub が利用者の識別子で、メールアドレスと表示名が含まれない")
    void tokenHasRequiredClaims() throws Exception {
        UserId userId = new UserId(UuidV7.at(NOW));

        JwtClaims claims = JwtClaims.parse(verifiedPayload(issuer.issue(userId, NOW)));

        assertThat(claims.getIssuer()).isEqualTo("auth-service");
        assertThat(claims.getAudience()).containsExactly("recipe-book");
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuedAt().getValue()).isEqualTo(NOW.getEpochSecond());
        assertThat(claims.getExpirationTime().getValue() - claims.getIssuedAt().getValue()).isEqualTo(60);
        assertThat(claims.getJwtId()).isNotBlank();
        // 本人の識別に要らない個人の情報と、役割を入れない
        assertThat(claims.getClaimNames()).containsOnly("iss", "aud", "sub", "iat", "exp", "jti");
    }

    @Test
    @DisplayName("トークンごとに jti が違う")
    void everyTokenHasDistinctId() throws Exception {
        UserId userId = new UserId(UuidV7.at(NOW));

        String first = JwtClaims.parse(verifiedPayload(issuer.issue(userId, NOW))).getJwtId();
        String second = JwtClaims.parse(verifiedPayload(issuer.issue(userId, NOW))).getJwtId();

        assertThat(first).isNotEqualTo(second);
    }

    /** 公開の鍵の一覧の今の鍵で署名を確かめ、中身を返す。署名が合わなければ失敗する。 */
    private static String verifiedPayload(String token) throws Exception {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(token);
        Map<String, Object> key = RestAssured.get("/internal/auth/keys").jsonPath().getMap("keys[0]");

        assertThat(jws.getAlgorithmHeaderValue()).isEqualTo("RS256");
        assertThat(jws.getKeyIdHeaderValue()).isEqualTo(key.get("kid"));
        jws.setKey(publicKeyOf((String) key.get("n"), (String) key.get("e")));
        assertThat(jws.verifySignature()).isTrue();
        return jws.getPayload();
    }

    private static PublicKey publicKeyOf(String n, String e) throws Exception {
        Base64.Decoder decoder = Base64.getUrlDecoder();
        return KeyFactory.getInstance("RSA").generatePublic(
                new RSAPublicKeySpec(new BigInteger(1, decoder.decode(n)), new BigInteger(1, decoder.decode(e))));
    }
}
