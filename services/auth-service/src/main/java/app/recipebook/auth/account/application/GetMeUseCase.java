package app.recipebook.auth.account.application;

import app.recipebook.auth.account.domain.UserRepository;
import app.recipebook.auth.shared.domain.UserId;
import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * ログインしている本人の情報を返す(「機能設計: 登録とログイン」の 4.6)。
 * 本人は、トークンの利用者の識別子だけで決める。ほかの利用者を指定する入力を持たない。
 */
@ApplicationScoped
public class GetMeUseCase {

    @Inject
    UserRepository users;

    /** 利用者がいないときは、UNAUTHENTICATED を投げる。消された人のトークンが残っている場合である。 */
    public UserView execute(UserId me) {
        return users.findById(me)
                .map(UserView::from)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
    }
}
