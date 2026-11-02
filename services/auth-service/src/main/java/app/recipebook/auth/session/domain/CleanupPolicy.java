package app.recipebook.auth.session.domain;

import java.time.Duration;
import java.time.Instant;

/** 期限が切れた行を消すときの決まり(「共通設計: サービスの共通構成」の 4.9、「機能設計: 登録とログイン」の 5.3)。 */
public final class CleanupPolicy {

    /** 1 回に消す行の数の上限。長い時間、行に鍵をかけないためである */
    public static final int BATCH_SIZE = 1_000;

    /**
     * ログインの失敗の記録を残す長さ。受け付けない時間(15 分)と数える時間より十分に長く、
     * これを過ぎた行は、もう判定に使わない。
     */
    public static final Duration LOGIN_ATTEMPT_RETENTION = Duration.ofDays(1);

    private CleanupPolicy() {
    }

    /** この瞬間以前に最後に更新したログインの失敗の記録を消す。 */
    public static Instant loginAttemptCutoff(Instant now) {
        return now.minus(LOGIN_ATTEMPT_RETENTION);
    }
}
