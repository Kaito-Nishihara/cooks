package app.recipebook.auth.shared.presentation;

import app.recipebook.auth.session.domain.SessionToken;
import jakarta.ws.rs.core.NewCookie;
import java.util.Optional;

/** セッションのクッキー(「共通設計: 認証」の 4.3)。属性を、ここ 1 か所で決める。 */
public final class SessionCookie {

    public static final String NAME = "rb_session";
    /** 30 日。ブラウザを閉じても残す */
    private static final int MAX_AGE_SECONDS = 2_592_000;

    private SessionCookie() {
    }

    /** 新しいセッションのクッキーを作る。Domain は付けない。発行したドメインにだけ送らせる。 */
    public static NewCookie issue(SessionToken token) {
        return base().value(token.reveal()).maxAge(MAX_AGE_SECONDS).build();
    }

    /** 要求に付いていたクッキーの値を、トークンとして読む。形が合わない値は、ないものとして扱う。 */
    public static Optional<SessionToken> read(String cookieValue) {
        return SessionToken.parse(cookieValue);
    }

    private static NewCookie.Builder base() {
        return new NewCookie.Builder(NAME)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite(NewCookie.SameSite.LAX);
    }
}
