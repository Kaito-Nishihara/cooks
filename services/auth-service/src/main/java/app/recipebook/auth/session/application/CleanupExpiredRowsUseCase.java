package app.recipebook.auth.session.application;

import app.recipebook.auth.session.domain.CleanupPolicy;
import app.recipebook.auth.session.domain.LoginAttemptRepository;
import app.recipebook.auth.session.domain.SessionRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.function.IntSupplier;
import org.jboss.logging.Logger;

/**
 * 期限が切れたセッションと、古いログインの失敗の記録を消す(「共通設計: サービスの共通構成」の 4.9)。
 * 期限が過ぎた行を消すだけなので、何度動かしても、複数のコンテナで同時に動いても、結果が同じである。
 */
@ApplicationScoped
public class CleanupExpiredRowsUseCase {

    private static final Logger LOG = Logger.getLogger(CleanupExpiredRowsUseCase.class);

    @Inject
    SessionRepository sessions;

    @Inject
    LoginAttemptRepository loginAttempts;

    @Inject
    Clock clock;

    public CleanupResult execute() {
        Instant now = clock.instant();
        int removedSessions = deleteInBatches(() -> sessions.deleteExpired(now, CleanupPolicy.BATCH_SIZE));
        Instant cutoff = CleanupPolicy.loginAttemptCutoff(now);
        int removedAttempts = deleteInBatches(
                () -> loginAttempts.deleteUpdatedBefore(cutoff, CleanupPolicy.BATCH_SIZE));
        LOG.infof("期限が切れた行を消しました。セッション: %d 行、ログインの失敗の記録: %d 行", removedSessions, removedAttempts);
        return new CleanupResult(removedSessions, removedAttempts);
    }

    /** 1 回ごとにトランザクションを分けて消し、上限より少なくなるまで繰り返す。消した行の合計を返す。 */
    private static int deleteInBatches(IntSupplier batch) {
        int total = 0;
        int removed;
        do {
            removed = QuarkusTransaction.requiringNew().call(batch::getAsInt);
            total += removed;
        } while (removed == CleanupPolicy.BATCH_SIZE);
        return total;
    }
}
