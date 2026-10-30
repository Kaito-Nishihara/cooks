package app.recipebook.auth.account.application;

import app.recipebook.auth.account.domain.User;

/**
 * ユースケースが返す、利用者の情報(「機能設計: 登録とログイン」の 4.2)。
 * パスワードのハッシュ、作成日時、セッションの情報は入れない。
 */
public record UserView(String id, String email, String displayName) {

    public static UserView from(User user) {
        return new UserView(user.id().toString(), user.email().value(), user.displayName().value());
    }
}
