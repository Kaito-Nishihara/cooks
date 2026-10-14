package app.recipebook.auth.account.application;

import app.recipebook.auth.account.domain.DisplayName;
import app.recipebook.auth.account.domain.EmailAddress;
import app.recipebook.auth.account.domain.PasswordHash;
import app.recipebook.auth.account.domain.PasswordHasher;
import app.recipebook.auth.account.domain.RawPassword;
import app.recipebook.auth.account.domain.User;
import app.recipebook.auth.account.domain.UserRepository;
import app.recipebook.auth.session.application.SessionIssuer;
import app.recipebook.auth.session.application.SignedIn;
import app.recipebook.auth.session.domain.SessionRepository;
import app.recipebook.auth.session.domain.SessionToken;
import app.recipebook.auth.shared.domain.UserId;
import app.recipebook.common.core.UuidV7;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;

/**
 * 登録して、ログインした状態にする(「機能設計: 登録とログイン」の 4.3)。
 * 作るのは利用者とセッションだけで、レシピ帳は作らない。
 */
@ApplicationScoped
public class RegisterUseCase {

    @Inject
    UserRepository users;

    @Inject
    SessionRepository sessions;

    @Inject
    SessionIssuer sessionIssuer;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    Clock clock;

    public SignedIn execute(RegisterCommand command) {
        DisplayName displayName = DisplayName.of(command.displayName());
        EmailAddress email = EmailAddress.of(command.email());
        RawPassword password = RawPassword.forNew(command.password());

        // ハッシュの計算には時間がかかるので、トランザクションを始める前に済ませる
        PasswordHash passwordHash = passwordHasher.hash(password);
        Instant now = clock.instant();

        return QuarkusTransaction.requiringNew().call(() -> {
            User user = User.register(new UserId(UuidV7.at(now)), email, displayName, passwordHash, now);
            // メールアドレスが登録済みなら、ここで EMAIL_TAKEN が投げられ、何も作られない
            users.add(user);
            SessionToken token = sessionIssuer.issue(user.id(), now);
            // ログインしたまま別のアカウントを登録した人の、前のセッションを残さない
            command.currentSession().ifPresent(previous -> sessions.deleteByTokenHash(previous.hash()));
            return new SignedIn(UserView.from(user), token);
        });
    }
}
