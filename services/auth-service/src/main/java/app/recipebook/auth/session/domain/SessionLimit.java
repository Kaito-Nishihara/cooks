package app.recipebook.auth.session.domain;

import java.util.Comparator;
import java.util.List;

/**
 * 1 人が同時に持てるセッションの数の決まり(「共通設計: 認証」の 4.3)。
 * 10 個までとし、11 個目を作るときは、最後に使った瞬間が一番古いものを消す。
 */
public final class SessionLimit {

    public static final int MAX_SESSIONS = 10;

    private SessionLimit() {
    }

    /** 新しいセッションを 1 つ足す前に、消すべきセッションを返す。 */
    public static List<Session> toEvictBeforeAdding(List<Session> current) {
        int excess = current.size() - (MAX_SESSIONS - 1);
        if (excess <= 0) {
            return List.of();
        }
        return current.stream()
                // 最後に使った瞬間が同じときに、消す相手が毎回同じになるように、識別子でも並べる
                .sorted(Comparator.comparing(Session::lastUsedAt)
                        .thenComparing(session -> session.id().value()))
                .limit(excess)
                .toList();
    }
}
