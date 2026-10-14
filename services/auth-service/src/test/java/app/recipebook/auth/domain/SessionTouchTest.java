package app.recipebook.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import app.recipebook.auth.session.domain.Session;
import app.recipebook.auth.session.domain.SessionId;
import app.recipebook.auth.session.domain.TokenHash;
import app.recipebook.auth.shared.domain.UserId;
import app.recipebook.common.core.UuidV7;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** セッションを使ったときの、期限の延ばし方(「共通設計: 認証」の 4.3)。 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SessionTouchTest {

    private static final Instant CREATED = Instant.parse("2026-10-10T03:00:00Z");

    private static Session newSession() {
        return Session.start(new SessionId(UuidV7.at(CREATED)), new UserId(UuidV7.at(CREATED)),
                new TokenHash("b".repeat(64)), CREATED);
    }

    @Test
    void 最後に使ってから_1_日たつまでは_記録しない() {
        Session session = newSession();

        assertThat(session.needsTouchAt(CREATED.plus(Duration.ofDays(1)).minusSeconds(1))).isFalse();
        assertThat(session.needsTouchAt(CREATED.plus(Duration.ofDays(1)))).isTrue();
    }

    @Test
    void 使うと_期限が使った瞬間の_30_日後に延びる() {
        Instant used = CREATED.plus(Duration.ofDays(10));

        Session touched = newSession().touchedAt(used);

        assertThat(touched.lastUsedAt()).isEqualTo(used);
        assertThat(touched.updatedAt()).isEqualTo(used);
        assertThat(touched.expiresAt()).isEqualTo(used.plus(Duration.ofDays(30)));
        assertThat(touched.createdAt()).isEqualTo(CREATED);
    }

    @Test
    void 延ばしても_作った瞬間の_90_日後を超えない() {
        Instant used = CREATED.plus(Duration.ofDays(75));

        Session touched = newSession().touchedAt(used);

        assertThat(touched.expiresAt()).isEqualTo(CREATED.plus(Duration.ofDays(90)));
        assertThat(touched.isValidAt(CREATED.plus(Duration.ofDays(90)))).isFalse();
    }
}
