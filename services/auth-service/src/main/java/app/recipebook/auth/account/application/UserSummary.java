package app.recipebook.auth.account.application;

import app.recipebook.auth.account.domain.User;

/**
 * ほかのサービスに渡す、利用者の情報(「機能設計: 登録とログイン」の 4.8)。
 * メンバーの一覧で、ほかの人のメールアドレスが見えないように、メールアドレスを入れない。
 */
public record UserSummary(String id, String displayName) {

    public static UserSummary from(User user) {
        return new UserSummary(user.id().toString(), user.displayName().value());
    }
}
