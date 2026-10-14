package app.recipebook.auth.session.application;

import app.recipebook.auth.session.domain.Session;
import app.recipebook.auth.session.domain.SessionRepository;
import app.recipebook.auth.session.domain.SessionToken;
import app.recipebook.auth.token.application.AccessTokenIssuer;
import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * 入口から呼ばれ、要求の送り主のセッションを確かめて、ほかのサービスへ渡すトークンを返す
 * (「共通設計: 認証」の 4.6)。
 */
@ApplicationScoped
public class VerifySessionUseCase {

    @Inject
    SessionRepository sessions;

    @Inject
    AccessTokenIssuer tokenIssuer;

    @Inject
    Clock clock;

    /** 有効なセッションなら、トークンを返す。ないか、切れていれば、UNAUTHENTICATED を投げる。 */
    public String execute(Optional<SessionToken> sessionToken) {
        Instant now = clock.instant();
        Session session = sessionToken
                .flatMap(token -> sessions.findByTokenHash(token.hash()))
                .filter(found -> found.isValidAt(now))
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (session.needsTouchAt(now)) {
            Session touched = session.touchedAt(now);
            QuarkusTransaction.requiringNew().run(() -> sessions.updateUsage(touched));
        }
        return tokenIssuer.issue(session.userId(), now);
    }
}
