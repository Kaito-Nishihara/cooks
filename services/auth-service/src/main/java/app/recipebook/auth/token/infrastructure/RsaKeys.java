package app.recipebook.auth.token.infrastructure;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** RSA の鍵のファイルを読み、鍵の名前を決める道具。 */
final class RsaKeys {

    /** 受け付ける鍵の長さの下限(「共通設計: 認証」の 4.8) */
    static final int MIN_BITS = 2048;

    private RsaKeys() {
    }

    /** PKCS#8 の PEM のファイルから、秘密の鍵を読む。 */
    static RSAPrivateCrtKey readPrivateKey(Path file) throws IOException, GeneralSecurityException {
        byte[] der = decodePem(Files.readString(file, StandardCharsets.US_ASCII), "PRIVATE KEY");
        if (!(KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der)) instanceof RSAPrivateCrtKey key)) {
            throw new GeneralSecurityException("RSA の秘密の鍵でない");
        }
        requireLength(key.getModulus());
        return key;
    }

    /** X.509 の PEM のファイルから、公開の鍵を読む。 */
    static RSAPublicKey readPublicKey(Path file) throws IOException, GeneralSecurityException {
        byte[] der = decodePem(Files.readString(file, StandardCharsets.US_ASCII), "PUBLIC KEY");
        RSAPublicKey key = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        requireLength(key.getModulus());
        return key;
    }

    /** 秘密の鍵から、対になる公開の鍵を作る。 */
    static RSAPublicKey publicKeyOf(RSAPrivateCrtKey privateKey) throws GeneralSecurityException {
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
    }

    /**
     * 鍵の名前を、公開の鍵から決める(RFC 7638 の JWK Thumbprint)。
     * 鍵ごとに違う名前になり、設定で名前を付けなくて済む。
     */
    static String keyIdOf(RSAPublicKey key) {
        String canonical = "{\"e\":\"" + base64Url(key.getPublicExponent())
                + "\",\"kty\":\"RSA\",\"n\":\"" + base64Url(key.getModulus()) + "\"}";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 符号なしの大きい数を、先頭の 0 を除いて、URL に使える Base64 にする(RFC 7518 の 6.3.1)。 */
    static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void requireLength(BigInteger modulus) throws GeneralSecurityException {
        if (modulus.bitLength() < MIN_BITS) {
            throw new GeneralSecurityException("RSA の鍵が " + MIN_BITS + " ビットより短い");
        }
    }

    private static byte[] decodePem(String pem, String label) throws GeneralSecurityException {
        String begin = "-----BEGIN " + label + "-----";
        String end = "-----END " + label + "-----";
        int start = pem.indexOf(begin);
        int stop = pem.indexOf(end);
        if (start < 0 || stop < start) {
            throw new GeneralSecurityException(label + " の PEM の形でない");
        }
        String body = pem.substring(start + begin.length(), stop).replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}
