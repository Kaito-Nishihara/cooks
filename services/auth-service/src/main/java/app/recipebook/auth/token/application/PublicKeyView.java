package app.recipebook.auth.token.application;

/**
 * 公開の鍵の 1 つ。RSA の公開の部分だけを持つ。
 *
 * @param keyId 鍵の名前。トークンのヘッダーの kid と同じ値
 * @param modulus RSA の法(n)を、URL に使える Base64 にしたもの
 * @param exponent RSA の公開の指数(e)を、URL に使える Base64 にしたもの
 */
public record PublicKeyView(String keyId, String modulus, String exponent) {
}
