package app.recipebook.auth.support;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * テスト用の時計。サービスの時計を、これに差し替える。
 * 時刻を固定し、進めることで、期限に関わる条件を、待たずに確かめる。
 */
@Alternative
@Priority(1)
@Singleton
public class MutableClock extends Clock {

    public static final Instant START = Instant.parse("2026-10-10T03:00:00Z");

    private volatile Instant now = START;

    /** 最初の時刻に戻す。テストの前に呼ぶ。 */
    public void reset() {
        now = START;
    }

    /**
     * 今の本物の時刻に合わせる。トークンを使うテストの前に呼ぶ。
     * トークンを確かめる部品は本物の時刻で期限を見るので、過去の時刻で作ったトークンは切れている。
     */
    public void resetToNow() {
        now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
