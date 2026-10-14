package app.recipebook.auth.session.domain;

import app.recipebook.auth.shared.domain.Sha256;

/** セッションのトークンの SHA-256 を、16 進の小文字 64 文字で表したもの。 */
public record TokenHash(String value) {

    public TokenHash {
        if (!Sha256.isHex(value)) {
            throw new IllegalArgumentException("SHA-256 の 16 進の形でない");
        }
    }
}
