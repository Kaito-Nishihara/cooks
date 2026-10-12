package app.recipebook.auth.session.domain;

import app.recipebook.auth.shared.domain.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * セッション(集約)(「共通設計: 認証」の 4.3)。
 * 最後に使ってから 30 日、作ってから 90 日で切れる。
 */
public final class Session {

    /** 最後に使ってから切れるまで */
    public static final Duration IDLE_LIFETIME = Duration.ofDays(30);
    /** 作ってから切れるまで。使い続けていても、ここで切れる */
    public static final Duration ABSOLUTE_LIFETIME = Duration.ofDays(90);

    private final SessionId id;
    private final UserId userId;
    private final TokenHash tokenHash;
    private final Instant createdAt;
    private final Instant lastUsedAt;
    private final Instant expiresAt;
    private final Instant updatedAt;

    private Session(SessionId id, UserId userId, TokenHash tokenHash, Instant createdAt, Instant lastUsedAt,
            Instant expiresAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.lastUsedAt = Objects.requireNonNull(lastUsedAt, "lastUsedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** 新しく作る。 */
    public static Session start(SessionId id, UserId userId, TokenHash tokenHash, Instant now) {
        return new Session(id, userId, tokenHash, now, now, expiryOf(now, now), now);
    }

    /** 保存してあった内容から組み立てる。リポジトリの実装だけが使う。 */
    public static Session restore(SessionId id, UserId userId, TokenHash tokenHash, Instant createdAt,
            Instant lastUsedAt, Instant expiresAt, Instant updatedAt) {
        return new Session(id, userId, tokenHash, createdAt, lastUsedAt, expiresAt, updatedAt);
    }

    /** 今の時刻で有効かどうかを返す。 */
    public boolean isValidAt(Instant now) {
        return now.isBefore(expiresAt);
    }

    /** 期限は、「最後に使った瞬間の 30 日後」と「作った瞬間の 90 日後」の、早いほうである。 */
    private static Instant expiryOf(Instant createdAt, Instant lastUsedAt) {
        Instant idleLimit = lastUsedAt.plus(IDLE_LIFETIME);
        Instant absoluteLimit = createdAt.plus(ABSOLUTE_LIFETIME);
        return idleLimit.isBefore(absoluteLimit) ? idleLimit : absoluteLimit;
    }

    public SessionId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public TokenHash tokenHash() {
        return tokenHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastUsedAt() {
        return lastUsedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
