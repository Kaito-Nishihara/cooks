package app.recipebook.auth.session.infrastructure;

import app.recipebook.auth.session.domain.Session;
import app.recipebook.auth.session.domain.SessionId;
import app.recipebook.auth.session.domain.TokenHash;
import app.recipebook.auth.shared.domain.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** sessions のテーブルの 1 行(「機能設計: 登録とログイン」の 5.2)。 */
@Entity
@Table(name = "sessions")
public class SessionJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    /** クッキーに入れた値の SHA-256。クッキーの値そのものは持たない */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, length = 64)
    String tokenHash;

    @Column(name = "last_used_at", nullable = false)
    Instant lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected SessionJpaEntity() {
        // JPA が使う
    }

    static SessionJpaEntity from(Session session) {
        SessionJpaEntity entity = new SessionJpaEntity();
        entity.id = session.id().value();
        entity.userId = session.userId().value();
        entity.tokenHash = session.tokenHash().value();
        entity.lastUsedAt = session.lastUsedAt();
        entity.expiresAt = session.expiresAt();
        entity.createdAt = session.createdAt();
        entity.updatedAt = session.updatedAt();
        return entity;
    }

    Session toDomain() {
        return Session.restore(new SessionId(id), new UserId(userId), new TokenHash(tokenHash), createdAt,
                lastUsedAt, expiresAt, updatedAt);
    }
}
