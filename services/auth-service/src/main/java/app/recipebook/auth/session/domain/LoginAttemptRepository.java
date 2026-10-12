package app.recipebook.auth.session.domain;

import java.time.Instant;
import java.util.Optional;

/** ログインの失敗の記録を保存し、取り出すための約束。 */
public interface LoginAttemptRepository {

    Optional<LoginAttempt> find(String emailHash);

    /**
     * 失敗を 1 回数える。行がなければ作り、あれば回数を増やす。
     * 数える時間(LoginLockPolicy.WINDOW)を過ぎていたら、1 から数え直す。
     * 回数が LoginLockPolicy.MAX_FAILURES に達したら、受け付けない期限を入れる。
     * 同じメールアドレスへの失敗が同時に届いても、回数を数え落とさない。
     */
    void recordFailure(String emailHash, Instant now);

    /** 記録を消す。ログインに成功したときに呼ぶ。なければ、何もしない。 */
    void clear(String emailHash);
}
