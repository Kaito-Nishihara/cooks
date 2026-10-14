package app.recipebook.auth.session.application;

import app.recipebook.auth.account.application.UserView;
import app.recipebook.auth.account.domain.EmailAddress;
import app.recipebook.auth.account.domain.PasswordHasher;
import app.recipebook.auth.account.domain.RawPassword;
import app.recipebook.auth.account.domain.User;
import app.recipebook.auth.account.domain.UserRepository;
import app.recipebook.auth.session.domain.LoginAttempt;
import app.recipebook.auth.session.domain.LoginAttemptRepository;
import app.recipebook.auth.session.domain.SessionRepository;
import app.recipebook.auth.session.domain.SessionToken;
import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * メールアドレスとパスワードでログインする(「機能設計: 登録とログイン」の 4.4)。
 * パスワードが違うときと、メールアドレスが登録されていないときとで、同じ応答にする。
 */
@ApplicationScoped
public class LoginUseCase {

    @Inject
    UserRepository users;

    @Inject
    SessionRepository sessions;

    @Inject
    LoginAttemptRepository loginAttempts;

    @Inject
    SessionIssuer sessionIssuer;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    Clock clock;

    public SignedIn execute(LoginCommand command) {
        EmailAddress email = EmailAddress.of(command.email());
        RawPassword password = RawPassword.forMatching(command.password());
        String emailHash = email.sha256();
        Instant now = clock.instant();

        // 受け付けない状態の間は、パスワードを確かめずに拒む。正しいパスワードでも通さない
        Optional<LoginAttempt> attempt = loginAttempts.find(emailHash);
        if (attempt.isPresent() && attempt.get().isLockedAt(now)) {
            throw new AppException(ErrorCode.LOGIN_LOCKED,
                    Map.of("retryAfterSeconds", attempt.get().retryAfterSeconds(now)));
        }

        // パスワードの照合には時間がかかるので、トランザクションの外で行う
        Optional<User> user = users.findByEmail(email);
        boolean matched;
        if (user.isPresent()) {
            matched = user.get().passwordMatches(password, passwordHasher);
        } else {
            // 登録がないときも、同じだけの計算を行う。応答の速さで、登録の有無が分からないようにする
            passwordHasher.burn(password);
            matched = false;
        }
        if (!matched) {
            // 登録のないメールアドレスでも、同じように数える
            QuarkusTransaction.requiringNew().run(() -> loginAttempts.recordFailure(emailHash, now));
            throw new AppException(ErrorCode.LOGIN_FAILED);
        }

        User signedInUser = user.get();
        return QuarkusTransaction.requiringNew().call(() -> {
            loginAttempts.clear(emailHash);
            SessionToken token = sessionIssuer.issue(signedInUser.id(), now);
            command.currentSession().ifPresent(previous -> sessions.deleteByTokenHash(previous.hash()));
            return new SignedIn(UserView.from(signedInUser), token);
        });
    }
}
