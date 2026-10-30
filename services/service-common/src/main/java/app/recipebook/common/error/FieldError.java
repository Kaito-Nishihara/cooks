package app.recipebook.common.error;

/**
 * 入力の誤りの 1 件。VALIDATION_FAILED の details.fields に入れる。
 *
 * @param path 入力の JSON の中の位置。クエリの変数のときは、変数の名前
 * @param code 誤りの種類
 * @param message 利用者に見せてよい文
 */
public record FieldError(String path, String code, String message) {

    public static FieldError of(String path, FieldErrorCode code) {
        return new FieldError(path, code.name(), code.message());
    }

    public static FieldError of(String path, FieldErrorCode code, String message) {
        return new FieldError(path, code.name(), message);
    }
}
