package app.recipebook.auth.account.domain;

import app.recipebook.auth.shared.domain.UserId;
import java.time.Instant;
import java.util.Objects;

/**
 * 利用者(集約)。
 * 保存する個人の情報は、メールアドレス、表示名、パスワードのハッシュに限る。
 */
public final class User {

    private final UserId id;
    private final EmailAddress email;
    private final DisplayName displayName;
    private final PasswordHash passwordHash;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(UserId id, EmailAddress email, DisplayName displayName, PasswordHash passwordHash,
            Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** 登録する。 */
    public static User register(UserId id, EmailAddress email, DisplayName displayName, PasswordHash passwordHash,
            Instant now) {
        return new User(id, email, displayName, passwordHash, now, now);
    }

    /** 保存してあった内容から組み立てる。リポジトリの実装だけが使う。 */
    public static User restore(UserId id, EmailAddress email, DisplayName displayName, PasswordHash passwordHash,
            Instant createdAt, Instant updatedAt) {
        return new User(id, email, displayName, passwordHash, createdAt, updatedAt);
    }

    /** パスワードが合うかを確かめる。 */
    public boolean passwordMatches(RawPassword password, PasswordHasher hasher) {
        return hasher.matches(password, passwordHash);
    }

    public UserId id() {
        return id;
    }

    public EmailAddress email() {
        return email;
    }

    public DisplayName displayName() {
        return displayName;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
