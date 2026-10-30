package app.recipebook.common.security;

import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * 要求の送り主(本人)。トークンの sub から取り出す(「共通設計: 認証」の 4.7)。
 * 入口の層と基盤の層が使い、ユースケースへは利用者の識別子として渡す。
 */
@RequestScoped
public class CurrentUser {

    @Inject
    JsonWebToken token;

    /** 本人の識別子を返す。トークンがないときは、UNAUTHENTICATED を投げる。 */
    public UUID id() {
        UUID id = parse(token.getSubject());
        if (id == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return id;
    }

    /** 受け取ったトークンの文字列。ほかのサービスを呼ぶときに、そのまま渡すために使う。ログに出さない。 */
    public String rawToken() {
        return token.getRawToken();
    }

    /** sub を UUID として読む。UUID の形でなければ null を返す。 */
    static UUID parse(String subject) {
        if (subject == null || subject.length() != 36) {
            return null;
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
