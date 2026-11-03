package app.recipebook.auth.account.presentation;

import app.recipebook.auth.account.domain.DisplayName;
import app.recipebook.auth.account.domain.EmailAddress;
import app.recipebook.auth.account.domain.RawPassword;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** 登録の入力の形。 */
public record RegisterRequest(
        @Schema(required = true, maxLength = DisplayName.MAX_LENGTH) @AccountInput(AccountInputKind.DISPLAY_NAME) String displayName,
        @Schema(required = true, maxLength = EmailAddress.MAX_LENGTH) @AccountInput(AccountInputKind.EMAIL) String email,
        @Schema(required = true, minLength = RawPassword.MIN_LENGTH, description = "UTF-8 で " + RawPassword.MAX_BYTES + " バイトまで") @AccountInput(AccountInputKind.NEW_PASSWORD) String password) {

    @Override
    public String toString() {
        return "RegisterRequest[隠した値]";
    }
}
