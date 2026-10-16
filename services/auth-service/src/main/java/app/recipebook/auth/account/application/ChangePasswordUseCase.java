package app.recipebook.auth.account.application;

import app.recipebook.auth.account.domain.PasswordHash;
import app.recipebook.auth.account.domain.PasswordHasher;
import app.recipebook.auth.account.domain.RawPassword;
import app.recipebook.auth.account.domain.User;
import app.recipebook.auth.account.domain.UserRepository;
import app.recipebook.auth.session.domain.LoginAttempt;
import app.recipebook.auth.session.domain.LoginAttemptRepository;
import app.recipebook.auth.session.domain.Session;
import app.recipebook.auth.session.domain.SessionRepository;
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
 * パスワードを変える(「機能設計: 登録とログイン」の 4.7)。
 * 変えると、今のセッションを除く、その人のすべてのセッションを無効にする(「共通設計: 認証」の 4.9)。
 */
@ApplicationScoped
public class ChangePasswordUseCase {

    @Inject
    UserRepository users;

    @Inject
    SessionRepository sessions;

    @Inject
    LoginAttemptRepository loginAttempts;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    Clock clock;

    public UserView execute(ChangePasswordCommand command) {
        RawPassword currentPassword = RawPassword.forMatching(command.currentPassword());
        RawPassword newPassword = RawPassword.forNew(command.newPassword());
        Instant now = clock.instant();

        // クッキーが指すセッションの利用者が、トークンの利用者と同じであることを確かめる
        Session session = command.currentSession()
                .flatMap(token -> sessions.findByTokenHash(token.hash()))
                .filter(found -> found.userId().equals(command.me()))
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        User user = users.findById(command.me())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        // ログインの失敗と同じ回数に数える。受け付けない間は、正しいパスワードでも変えさせない
        String emailHash = user.email().sha256();
        Optional<LoginAttempt> attempt = loginAttempts.find(emailHash);
        if (attempt.isPresent() && attempt.get().isLockedAt(now)) {
            throw new AppException(ErrorCode.LOGIN_LOCKED,
                    Map.of("retryAfterSeconds", attempt.get().retryAfterSeconds(now)));
        }
        if (!user.passwordMatches(currentPassword, passwordHasher)) {
            QuarkusTransaction.requiringNew().run(() -> loginAttempts.recordFailure(emailHash, now));
            throw new AppException(ErrorCode.LOGIN_FAILED);
        }

        // ハッシュの計算には時間がかかるので、トランザクションを始める前に済ませる
        PasswordHash newHash = passwordHasher.hash(newPassword);
        User changed = user.withPassword(newHash, now);
        QuarkusTransaction.requiringNew().run(() -> {
            users.updatePassword(changed);
            sessions.deleteAllOfUserExcept(changed.id(), session.id());
            loginAttempts.clear(emailHash);
        });
        return UserView.from(changed);
    }
}
