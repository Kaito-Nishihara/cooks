package app.recipebook.auth.session.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 1 つのメールアドレスの、ログインの失敗の記録(集約)。
 * メールアドレスそのものではなく、ハッシュで持つ。登録のないメールアドレスも、同じように記録するためである。
 */
public final class LoginAttempt {

    private final String emailHash;
    private final int failedCount;
    private final Instant windowStartedAt;
    private final Instant lockedUntil;

    private LoginAttempt(String emailHash, int failedCount, Instant windowStartedAt, Instant lockedUntil) {
        this.emailHash = Objects.requireNonNull(emailHash, "emailHash");
        this.failedCount = failedCount;
        this.windowStartedAt = Objects.requireNonNull(windowStartedAt, "windowStartedAt");
        this.lockedUntil = lockedUntil;
    }

    /** 保存してあった内容から組み立てる。リポジトリの実装だけが使う。 */
    public static LoginAttempt restore(String emailHash, int failedCount, Instant windowStartedAt,
            Instant lockedUntil) {
        return new LoginAttempt(emailHash, failedCount, windowStartedAt, lockedUntil);
    }

    /** 今の時刻で、受け付けない状態かどうかを返す。 */
    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /** 受け付けない期限までの秒を、切り上げて返す。1 以上、受け付けない時間の秒数以下になる。 */
    public long retryAfterSeconds(Instant now) {
        if (!isLockedAt(now)) {
            return 0;
        }
        Duration remaining = Duration.between(now, lockedUntil);
        long seconds = remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0);
        return Math.max(1, Math.min(seconds, LoginLockPolicy.LOCK.getSeconds()));
    }

    public String emailHash() {
        return emailHash;
    }

    public int failedCount() {
        return failedCount;
    }

    public Instant windowStartedAt() {
        return windowStartedAt;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }
}
