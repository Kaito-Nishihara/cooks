package app.recipebook.auth.session.presentation;

import app.recipebook.auth.account.domain.EmailAddress;
import app.recipebook.auth.account.presentation.AccountInput;
import app.recipebook.auth.account.presentation.AccountInputKind;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** ログインの入力の形。パスワードに、長さの下限を適用しない。 */
public record LoginRequest(
        @Schema(required = true, maxLength = EmailAddress.MAX_LENGTH) @AccountInput(AccountInputKind.EMAIL) String email,
        @Schema(required = true) @AccountInput(AccountInputKind.PASSWORD) String password) {

    @Override
    public String toString() {
        return "LoginRequest[隠した値]";
    }
}
