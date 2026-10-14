package app.recipebook.auth.account.presentation;

import app.recipebook.auth.account.application.UserView;

/** 利用者の情報の形(「機能設計: 登録とログイン」の 4.2)。項目は、この 3 つだけである。 */
public record UserResponse(String id, String email, String displayName) {

    public static UserResponse from(UserView user) {
        return new UserResponse(user.id(), user.email(), user.displayName());
    }
}
