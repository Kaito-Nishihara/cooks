package app.recipebook.auth.session.domain;

import app.recipebook.auth.shared.domain.Sha256;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * セッションのトークン。クッキーに入れる値で、256 ビットの乱数を URL に使える文字にした 43 文字である。
 * 保存もログ出力もしない。データベースには、ハッシュだけを保存する。
 */
public final class SessionToken {

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");

    private final String value;

    public SessionToken(String value) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("セッションのトークンの形でない");
        }
        this.value = value;
    }

    /** クッキーの値から作る。形が合わない値は、セッションを指していないものとして扱う。 */
    public static Optional<SessionToken> parse(String cookieValue) {
        if (cookieValue == null || !FORMAT.matcher(cookieValue).matches()) {
            return Optional.empty();
        }
        return Optional.of(new SessionToken(cookieValue));
    }

    /** データベースに保存し、探すときに使うハッシュを返す。 */
    public TokenHash hash() {
        return new TokenHash(Sha256.hex(value));
    }

    /** クッキーに入れる値。ほかの用途に使わない。 */
    public String reveal() {
        return value;
    }

    @Override
    public String toString() {
        return "SessionToken[隠した値]";
    }
}
