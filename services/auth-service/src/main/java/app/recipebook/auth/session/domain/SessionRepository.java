package app.recipebook.auth.session.domain;

import app.recipebook.auth.shared.domain.UserId;
import java.util.List;

/** セッションを保存し、取り出すための約束。 */
public interface SessionRepository {

    void add(Session session);

    /** その人のセッションを、すべて返す。1 人のセッションは 10 個までなので、上限を付けずに返してよい。 */
    List<Session> listByUser(UserId userId);

    void delete(SessionId id);

    /** トークンのハッシュが指すセッションを消す。なければ、何もしない。 */
    void deleteByTokenHash(TokenHash tokenHash);
}
