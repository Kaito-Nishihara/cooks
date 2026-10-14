package app.recipebook.auth.session.infrastructure;

import app.recipebook.auth.session.domain.LoginAttempt;
import app.recipebook.auth.session.domain.LoginAttemptRepository;
import app.recipebook.auth.session.domain.LoginLockPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;

/** ログインの失敗の記録のリポジトリの実装。 */
@ApplicationScoped
public class JpaLoginAttemptRepository implements LoginAttemptRepository {

    /**
     * 行がなければ作り、あれば回数を増やすことを、1 つの命令で行う。
     * 同じメールアドレスへの失敗が同時に届いても、回数を数え落とさないためである。
     * 数える時間を過ぎた行は、1 から数え直す。
     */
    private static final String RECORD_FAILURE = """
            INSERT INTO login_attempts
                (email_hash, failed_count, window_started_at, locked_until, created_at, updated_at)
            VALUES (:emailHash, 1, :now, NULL, :now, :now)
            ON CONFLICT (email_hash) DO UPDATE SET
                failed_count = CASE
                    WHEN login_attempts.window_started_at <= :windowLimit THEN 1
                    ELSE login_attempts.failed_count + 1 END,
                window_started_at = CASE
                    WHEN login_attempts.window_started_at <= :windowLimit THEN :now
                    ELSE login_attempts.window_started_at END,
                locked_until = CASE
                    WHEN login_attempts.window_started_at <= :windowLimit THEN NULL
                    WHEN login_attempts.failed_count + 1 >= :maxFailures THEN :lockUntil
                    ELSE login_attempts.locked_until END,
                updated_at = :now
            """;

    @Inject
    EntityManager entityManager;

    @Override
    public Optional<LoginAttempt> find(String emailHash) {
        return Optional.ofNullable(entityManager.find(LoginAttemptJpaEntity.class, emailHash))
                .map(LoginAttemptJpaEntity::toDomain);
    }

    @Override
    public void recordFailure(String emailHash, Instant now) {
        entityManager.createNativeQuery(RECORD_FAILURE)
                .setParameter("emailHash", emailHash)
                .setParameter("now", now)
                .setParameter("windowLimit", now.minus(LoginLockPolicy.WINDOW))
                .setParameter("maxFailures", LoginLockPolicy.MAX_FAILURES)
                .setParameter("lockUntil", now.plus(LoginLockPolicy.LOCK))
                .executeUpdate();
    }

    @Override
    public void clear(String emailHash) {
        entityManager.createQuery("DELETE FROM LoginAttemptJpaEntity a WHERE a.emailHash = :emailHash")
                .setParameter("emailHash", emailHash)
                .executeUpdate();
    }
}
