package app.recipebook.common.testing;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * テスト用のトークンを作る道具(「共通設計: サービスの共通構成」の 7 章)。
 * テストを動かすたびに鍵の組を作るので、リポジトリに鍵を置かない。
 * 公開の鍵は、jwks() の値を、テストのアプリの代役から配る。
 * テストだけで使う。動かすときのコードから呼ばない。
 */
public final class TestTokens {

    public static final String KEY_ID = "test-key";
    private static final KeyPair KEYS = newKeyPair();
    private static final KeyPair OTHER_KEYS = newKeyPair();

    private TestTokens() {
    }

    /** この利用者の、今から 60 秒有効なトークンを作る。 */
    public static String forUser(UUID userId) {
        return signedAt(userId.toString(), Instant.now(), 60, KEYS, "auth-service", "recipe-book");
    }

    /** 期限を指定の秒だけ過ぎたトークンを作る。 */
    public static String expiredSecondsAgo(UUID userId, long seconds) {
        Instant expiresAt = Instant.now().minusSeconds(seconds);
        return signedAt(userId.toString(), expiresAt.minusSeconds(60), 60, KEYS, "auth-service", "recipe-book");
    }

    /** 配っていない別の鍵で署名したトークンを作る。 */
    public static String signedWithOtherKey(UUID userId) {
        return signedAt(userId.toString(), Instant.now(), 60, OTHER_KEYS, "auth-service", "recipe-book");
    }

    /** 発行者、使う場所、sub を指定して作る。決まりに合わないトークンを作るのに使う。 */
    public static String withClaims(String subject, String issuer, String audience) {
        return signedAt(subject, Instant.now(), 60, KEYS, issuer, audience);
    }

    /** 配る公開の鍵の一覧(JWKS)。 */
    public static String jwks() {
        RSAPublicKey key = (RSAPublicKey) KEYS.getPublic();
        return "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"" + KEY_ID
                + "\",\"n\":\"" + base64Url(key.getModulus()) + "\",\"e\":\"" + base64Url(key.getPublicExponent())
                + "\"}]}";
    }

    private static String signedAt(String subject, Instant issuedAt, long lifetimeSeconds, KeyPair keys,
            String issuer, String audience) {
        return Jwt.issuer(issuer)
                .audience(audience)
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(lifetimeSeconds))
                .claim("jti", UUID.randomUUID().toString())
                .jws()
                .keyId(KEY_ID)
                .algorithm(SignatureAlgorithm.RS256)
                .sign(keys.getPrivate());
    }

    private static KeyPair newKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        int offset = bytes.length > 1 && bytes[0] == 0 ? 1 : 0;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(java.util.Arrays.copyOfRange(bytes, offset, bytes.length));
    }
}
