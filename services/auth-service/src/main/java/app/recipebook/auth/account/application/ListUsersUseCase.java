package app.recipebook.auth.account.application;

import app.recipebook.auth.account.domain.UserRepository;
import app.recipebook.auth.shared.domain.UserId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 利用者の表示名を、識別子でまとめて返す(「機能設計: 登録とログイン」の 4.8)。
 * ほかのサービスが、メンバーの一覧に出す表示名を取るために呼ぶ。
 * トークンの利用者が誰であるかは、条件にしない。
 */
@ApplicationScoped
public class ListUsersUseCase {

    @Inject
    UserRepository users;

    /** 見つかった利用者を、識別子の昇順で返す。見つからない識別子は飛ばし、同じ識別子は 1 件にする。 */
    public List<UserSummary> execute(List<UserId> ids) {
        return users.findAllById(new LinkedHashSet<>(ids)).stream()
                .map(UserSummary::from)
                .toList();
    }
}
