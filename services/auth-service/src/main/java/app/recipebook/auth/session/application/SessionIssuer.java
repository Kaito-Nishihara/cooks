package app.recipebook.auth.session.application;

import app.recipebook.auth.account.domain.UserRepository;
import app.recipebook.auth.session.domain.Session;
import app.recipebook.auth.session.domain.SessionId;
import app.recipebook.auth.session.domain.SessionLimit;
import app.recipebook.auth.session.domain.SessionRepository;
import app.recipebook.auth.session.domain.SessionToken;
import app.recipebook.auth.session.domain.SessionTokenGenerator;
import app.recipebook.auth.shared.domain.UserId;
import app.recipebook.common.core.UuidV7;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;

/**
 * セッションを作る。登録とログインの両方が使う。
 * 呼ぶ側のトランザクションの中で動かす。
 */
@ApplicationScoped
public class SessionIssuer {

    @Inject
    UserRepository users;

    @Inject
    SessionRepository sessions;

    @Inject
    SessionTokenGenerator tokenGenerator;

    /** 新しいセッションを作り、クッキーに入れるトークンを返す。ログインのたびに、新しいトークンを作る。 */
    public SessionToken issue(UserId userId, Instant now) {
        // 同じ人のログインが同時に届いても、数の上限を超えないように、順番に行わせる
        users.lock(userId);
        SessionLimit.toEvictBeforeAdding(sessions.listByUser(userId))
                .forEach(session -> sessions.delete(session.id()));

        SessionToken token = tokenGenerator.generate();
        sessions.add(Session.start(new SessionId(UuidV7.at(now)), userId, token.hash(), now));
        return token;
    }
}
