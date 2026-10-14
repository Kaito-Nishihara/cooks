package app.recipebook.auth.account.presentation;

/** 登録の入力の形。 */
public record RegisterRequest(
        @AccountInput(AccountInputKind.DISPLAY_NAME) String displayName,
        @AccountInput(AccountInputKind.EMAIL) String email,
        @AccountInput(AccountInputKind.NEW_PASSWORD) String password) {

    @Override
    public String toString() {
        return "RegisterRequest[隠した値]";
    }
}
