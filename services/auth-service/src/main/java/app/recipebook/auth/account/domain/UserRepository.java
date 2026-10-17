package app.recipebook.auth.account.domain;

import app.recipebook.auth.shared.domain.UserId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 利用者を保存し、取り出すための約束。 */
public interface UserRepository {

    /**
     * 新しい利用者を保存する。
     * メールアドレスが登録済みのときは、何も保存せずに、EMAIL_TAKEN の AppException を投げる。
     * 同時に 2 つ届いても、1 つだけが成功する。
     */
    void add(User user);

    Optional<User> findByEmail(EmailAddress email);

    Optional<User> findById(UserId id);

    /** 識別子が合う利用者を、識別子の昇順で返す。見つからない識別子は飛ばす。 */
    List<User> findAllById(Collection<UserId> ids);

    /** パスワードのハッシュと、更新日時を書き換える。 */
    void updatePassword(User user);

    /**
     * この利用者に関わる書き込みを、順番に行わせるために、鍵をかける。
     * トランザクションが終わるまで、同じ利用者への別の書き込みが待つ。
     */
    void lock(UserId id);
}
