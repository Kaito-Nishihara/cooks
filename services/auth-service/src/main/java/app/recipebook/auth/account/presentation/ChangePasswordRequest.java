package app.recipebook.auth.account.presentation;

/** パスワードの変更の入力の形。今のパスワードには、長さの下限を適用しない。 */
public record ChangePasswordRequest(
        @AccountInput(AccountInputKind.PASSWORD) String currentPassword,
        @AccountInput(AccountInputKind.NEW_PASSWORD) String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[隠した値]";
    }
}
