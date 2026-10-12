package app.recipebook.auth.account.infrastructure;

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
}
