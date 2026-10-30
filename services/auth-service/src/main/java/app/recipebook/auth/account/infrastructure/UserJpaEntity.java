package app.recipebook.auth.account.infrastructure;

import app.recipebook.auth.account.domain.DisplayName;
import app.recipebook.auth.account.domain.EmailAddress;
import app.recipebook.auth.account.domain.PasswordHash;
import app.recipebook.auth.account.domain.User;
import app.recipebook.auth.shared.domain.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * users のテーブルの 1 行(「機能設計: 登録とログイン」の 5.1)。
 * 集約とは別のクラスで、リポジトリの実装だけが使う。業務の決まりを書かない。
 */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "email", nullable = false, length = 254)
    String email;

    @Column(name = "display_name", nullable = false, length = 50)
    String displayName;

    /** bcrypt の出力。長さが決まっているので char(60) で持つ */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "password_hash", nullable = false, length = 60)
    String passwordHash;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected UserJpaEntity() {
        // JPA が使う
    }

    static UserJpaEntity from(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = user.id().value();
        entity.email = user.email().value();
        entity.displayName = user.displayName().value();
        entity.passwordHash = user.passwordHash().value();
        entity.createdAt = user.createdAt();
        entity.updatedAt = user.updatedAt();
        return entity;
    }

    User toDomain() {
        return User.restore(new UserId(id), new EmailAddress(email), new DisplayName(displayName),
                new PasswordHash(passwordHash), createdAt, updatedAt);
    }
}
