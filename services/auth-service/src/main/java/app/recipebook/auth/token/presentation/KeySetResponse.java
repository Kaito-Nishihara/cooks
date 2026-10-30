package app.recipebook.auth.token.presentation;

import app.recipebook.auth.token.application.PublicKeyView;
import java.util.List;

/** 公開の鍵の一覧(JWKS、RFC 7517)。公開の部分だけを入れる。 */
public record KeySetResponse(List<Key> keys) {

    /** 鍵の 1 つ。項目の名前は、JWKS の決まりのとおりである。 */
    public record Key(String kty, String use, String alg, String kid, String n, String e) {
    }

    public static KeySetResponse from(List<PublicKeyView> views) {
        return new KeySetResponse(views.stream()
                .map(view -> new Key("RSA", "sig", "RS256", view.keyId(), view.modulus(), view.exponent()))
                .toList());
    }
}
