package app.recipebook.auth.session.application;

import app.recipebook.auth.session.domain.Session;
import app.recipebook.auth.session.domain.SessionRepository;
import app.recipebook.auth.session.domain.SessionToken;
import app.recipebook.auth.shared.domain.UserId;
import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;

/**
 * 今のセッションを無効にする(「機能設計: 登録とログイン」の 4.5)。
 * 消すのは、クッキーが指すセッションだけである。ほかの端末のセッションは残す。
 */
@ApplicationScoped
public class LogoutUseCase {

    @Inject
    SessionRepository sessions;

    /**
     * クッキーが指すセッションを消す。
     * クッキーがないか、セッションの利用者がトークンの利用者と違うときは、UNAUTHENTICATED を投げる。
     * セッションの行がもうないときは、何もせずに終える。
     */
    public void execute(UserId me, Optional<SessionToken> sessionToken) {
        SessionToken token = sessionToken.orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        Optional<Session> session = sessions.findByTokenHash(token.hash());
        if (session.isEmpty()) {
            return;
        }
        if (!session.get().userId().equals(me)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        QuarkusTransaction.requiringNew().run(() -> sessions.delete(session.get().id()));
    }
}
