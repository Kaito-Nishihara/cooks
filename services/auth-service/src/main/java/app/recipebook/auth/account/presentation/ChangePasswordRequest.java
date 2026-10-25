package app.recipebook.auth.account.presentation;

import app.recipebook.auth.account.domain.RawPassword;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** パスワードの変更の入力の形。今のパスワードには、長さの下限を適用しない。 */
public record ChangePasswordRequest(
        @Schema(required = true) @AccountInput(AccountInputKind.PASSWORD) String currentPassword,
        @Schema(required = true, minLength = RawPassword.MIN_LENGTH, description = "UTF-8 で " + RawPassword.MAX_BYTES + " バイトまで") @AccountInput(AccountInputKind.NEW_PASSWORD) String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[隠した値]";
    }
}
