package app.recipebook.auth.session.infrastructure;

import app.recipebook.auth.session.domain.LoginAttempt;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * login_attempts のテーブルの 1 行(「機能設計: 登録とログイン」の 5.3)。
 * 主キーは id ではなく、メールアドレスのハッシュである。
 */
@Entity
@Table(name = "login_attempts")
public class LoginAttemptJpaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "email_hash", nullable = false, length = 64)
    String emailHash;

    @Column(name = "failed_count", nullable = false)
    int failedCount;

    @Column(name = "window_started_at", nullable = false)
    Instant windowStartedAt;

    @Column(name = "locked_until")
    Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected LoginAttemptJpaEntity() {
        // JPA が使う
    }

    LoginAttempt toDomain() {
        return LoginAttempt.restore(emailHash, failedCount, windowStartedAt, lockedUntil);
    }
}
