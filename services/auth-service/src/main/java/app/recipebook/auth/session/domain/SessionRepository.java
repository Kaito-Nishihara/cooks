package app.recipebook.auth.session.domain;

import app.recipebook.auth.shared.domain.UserId;
import java.util.List;
import java.util.Optional;

/** セッションを保存し、取り出すための約束。 */
public interface SessionRepository {

    void add(Session session);

    /** トークンのハッシュが指すセッションを返す。 */
    Optional<Session> findByTokenHash(TokenHash tokenHash);

    /** 使ったことの記録(最後に使った瞬間、期限、更新日時)を書き換える。 */
    void updateUsage(Session session);

    /** その人のセッションを、すべて返す。1 人のセッションは 10 個までなので、上限を付けずに返してよい。 */
    List<Session> listByUser(UserId userId);

    void delete(SessionId id);

    /** トークンのハッシュが指すセッションを消す。なければ、何もしない。 */
    void deleteByTokenHash(TokenHash tokenHash);
}
