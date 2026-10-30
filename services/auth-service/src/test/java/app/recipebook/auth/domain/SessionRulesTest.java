package app.recipebook.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import app.recipebook.auth.session.domain.LoginAttempt;
import app.recipebook.auth.session.domain.Session;
import app.recipebook.auth.session.domain.SessionId;
import app.recipebook.auth.session.domain.SessionLimit;
import app.recipebook.auth.session.domain.TokenHash;
import app.recipebook.auth.shared.domain.UserId;
import app.recipebook.common.core.UuidV7;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** セッションと、ログインの失敗の記録の決まり(「共通設計: 認証」の 4.3 と 4.4)。 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SessionRulesTest {

    private static final Instant NOW = Instant.parse("2026-10-10T03:00:00Z");
    private static final UserId USER = new UserId(UuidV7.at(NOW));
    private static final TokenHash HASH = new TokenHash("a".repeat(64));

    private static Session sessionStartedAt(Instant startedAt) {
        return Session.start(new SessionId(UuidV7.at(startedAt)), USER, HASH, startedAt);
    }

    @Test
    void セッションは_作った瞬間の_30_日後に切れる() {
        Session session = sessionStartedAt(NOW);

        assertThat(session.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(session.isValidAt(NOW.plus(Duration.ofDays(30)).minusSeconds(1))).isTrue();
        assertThat(session.isValidAt(NOW.plus(Duration.ofDays(30)))).isFalse();
    }

    @Test
    void セッションが_9_個までなら_消すものはない() {
        List<Session> current = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            current.add(sessionStartedAt(NOW.plusSeconds(i)));
        }
        assertThat(SessionLimit.toEvictBeforeAdding(current)).isEmpty();
    }

    @Test
    void セッションが_10_個あるときは_最後に使った瞬間が一番古いものを消す() {
        List<Session> current = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            current.add(sessionStartedAt(NOW.plusSeconds(10 - i)));
        }
        Session oldest = current.get(9);

        assertThat(SessionLimit.toEvictBeforeAdding(current)).containsExactly(oldest);
    }

    @Test
    void 受け付けない期限までの秒は_切り上げる() {
        LoginAttempt locked = LoginAttempt.restore("a".repeat(64), 5, NOW, NOW.plus(Duration.ofMinutes(15)));

        assertThat(locked.isLockedAt(NOW)).isTrue();
        assertThat(locked.retryAfterSeconds(NOW)).isEqualTo(900);
        assertThat(locked.retryAfterSeconds(NOW.plusMillis(1))).isEqualTo(900);
        assertThat(locked.retryAfterSeconds(NOW.plusSeconds(899).plusMillis(999))).isEqualTo(1);
        assertThat(locked.isLockedAt(NOW.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void 受け付けない期限がない記録は_受け付ける() {
        LoginAttempt counting = LoginAttempt.restore("a".repeat(64), 4, NOW, null);

        assertThat(counting.isLockedAt(NOW)).isFalse();
        assertThat(counting.retryAfterSeconds(NOW)).isZero();
    }

    @Test
    void 識別子は_バージョン_7_で_作った順に並ぶ() {
        var earlier = UuidV7.at(NOW);
        var later = UuidV7.at(NOW.plusMillis(1));

        assertThat(earlier.version()).isEqualTo(7);
        assertThat(earlier.variant()).isEqualTo(2);
        assertThat(earlier.toString().compareTo(later.toString())).isNegative();
    }
}
