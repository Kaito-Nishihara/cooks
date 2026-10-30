package app.recipebook.auth.session.presentation;

import app.recipebook.auth.account.presentation.AccountInput;
import app.recipebook.auth.account.presentation.AccountInputKind;

/** ログインの入力の形。パスワードに、長さの下限を適用しない。 */
public record LoginRequest(
        @AccountInput(AccountInputKind.EMAIL) String email,
        @AccountInput(AccountInputKind.PASSWORD) String password) {

    @Override
    public String toString() {
        return "LoginRequest[隠した値]";
    }
}
