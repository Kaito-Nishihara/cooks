package app.recipebook.auth.session;

import static org.assertj.core.api.Assertions.assertThat;

import app.recipebook.auth.session.application.CleanupExpiredRowsUseCase;
import app.recipebook.auth.session.application.CleanupResult;
import app.recipebook.auth.support.MutableClock;
import app.recipebook.auth.support.TestDatabase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 期限が切れた行を消す定期の処理(「共通設計: サービスの共通構成」の 4.9)。 */
@QuarkusTest
class CleanupTest {

    private static final String USER_ID = "0192f3a1-7c2e-7b4a-9d1e-3f5a6b7c8d9e";

    @Inject
    MutableClock clock;

    @Inject
    CleanupExpiredRowsUseCase cleanup;

    @BeforeEach
    void setUp() {
        clock.reset();
        TestDatabase.truncateAll();
        TestDatabase.update("INSERT INTO users (id, email, display_name, password_hash, created_at, updated_at)"
                + " VALUES ('" + USER_ID + "', 'misaki@example.com', 'みさき', repeat('x', 60), now(), now())");
    }

    /** 期限を now からずらしたセッションを、count 行作る。 */
    private void insertSessions(int count, Duration fromNow) {
        Instant expiresAt = MutableClock.START.plus(fromNow);
        TestDatabase.update("INSERT INTO sessions (id, user_id, token_hash, last_used_at, expires_at, created_at, updated_at)"
                + " SELECT gen_random_uuid(), '" + USER_ID + "', md5(random()::text) || md5(random()::text),"
                + " timestamptz '" + expiresAt + "' - interval '1 day', timestamptz '" + expiresAt + "',"
                + " timestamptz '" + expiresAt + "' - interval '1 day', timestamptz '" + expiresAt + "' - interval '1 day'"
                + " FROM generate_series(1, " + count + ")");
    }

    /** 最後に更新した瞬間を now からずらしたログインの失敗の記録を、count 行作る。 */
    private void insertLoginAttempts(int count, Duration fromNow) {
        Instant updatedAt = MutableClock.START.plus(fromNow);
        TestDatabase.update("INSERT INTO login_attempts (email_hash, failed_count, window_started_at, locked_until, created_at, updated_at)"
                + " SELECT md5(random()::text) || md5(random()::text), 1, timestamptz '" + updatedAt + "', NULL,"
                + " timestamptz '" + updatedAt + "', timestamptz '" + updatedAt + "'"
                + " FROM generate_series(1, " + count + ")");
    }

    @Test
    @DisplayName("AC-SVC-17 期限が切れたセッションを 2,500 行作って定期の処理を動かすと、すべて消え、期限が切れていない行は残る")
    void removesAllExpiredSessions() {
        insertSessions(2_500, Duration.ofHours(-1));
        insertSessions(3, Duration.ofHours(1));

        CleanupResult result = cleanup.execute();

        assertThat(result.sessions()).isEqualTo(2_500);
        assertThat(TestDatabase.count("sessions")).isEqualTo(3);
        assertThat(TestDatabase.queryStrings("SELECT count(*) FROM sessions WHERE expires_at <= '"
                + MutableClock.START + "'")).containsExactly("0");
    }

    @Test
    @DisplayName("期限がちょうど今のセッションは、もう使えないので消す")
    void removesSessionExpiringNow() {
        insertSessions(1, Duration.ZERO);
        insertSessions(1, Duration.ofSeconds(1));

        cleanup.execute();

        assertThat(TestDatabase.count("sessions")).isEqualTo(1);
    }

    @Test
    @DisplayName("ログインの失敗の記録は、最後の更新が 1 日以上前の行だけを消す")
    void removesLoginAttemptsOlderThanOneDay() {
        insertLoginAttempts(2, Duration.ofHours(-25));
        insertLoginAttempts(1, Duration.ofDays(-1));
        insertLoginAttempts(4, Duration.ofHours(-23));

        CleanupResult result = cleanup.execute();

        assertThat(result.loginAttempts()).isEqualTo(3);
        assertThat(TestDatabase.count("login_attempts")).isEqualTo(4);
    }

    @Test
    @DisplayName("続けて 2 回動かしても、2 回目は何も消さず、結果が変わらない")
    void runningTwiceIsHarmless() {
        insertSessions(1_200, Duration.ofHours(-1));
        insertSessions(2, Duration.ofHours(1));
        insertLoginAttempts(3, Duration.ofDays(-2));

        cleanup.execute();
        CleanupResult second = cleanup.execute();

        assertThat(second).isEqualTo(new CleanupResult(0, 0));
        assertThat(TestDatabase.count("sessions")).isEqualTo(2);
        assertThat(TestDatabase.count("login_attempts")).isZero();
    }
}
