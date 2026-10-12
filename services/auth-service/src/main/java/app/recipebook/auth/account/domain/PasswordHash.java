package app.recipebook.auth.account.domain;

/** パスワードのハッシュ。bcrypt の出力で、60 文字、「$2」で始まる。 */
public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.length() != 60 || !value.startsWith("$2")) {
            throw new IllegalArgumentException("bcrypt のハッシュの形でない");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[隠した値]";
    }
}
