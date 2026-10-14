package app.recipebook.auth.session.domain;

import java.time.Duration;

/**
 * ログインの失敗の制限の数字(「共通設計: 認証」の 4.4)。
 * 15 分のうちに 5 回失敗したメールアドレスを、15 分間受け付けない。
 */
public final class LoginLockPolicy {

    /** 失敗を数える時間 */
    public static final Duration WINDOW = Duration.ofMinutes(15);
    /** 受け付けなくなる失敗の回数 */
    public static final int MAX_FAILURES = 5;
    /** 受け付けない時間 */
    public static final Duration LOCK = Duration.ofMinutes(15);

    private LoginLockPolicy() {
    }
}
