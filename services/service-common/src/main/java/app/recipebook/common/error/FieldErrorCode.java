package app.recipebook.common.error;

/** 入力の誤りの種類(「共通設計: API の共通仕様」の 4.9)。 */
public enum FieldErrorCode {

    REQUIRED("入力してください。"),
    MIN("値が小さすぎます。"),
    MAX("値が大きすぎます。"),
    TOO_SHORT("短すぎます。"),
    TOO_LONG("長すぎます。"),
    TOO_MANY("件数が多すぎます。"),
    INVALID_FORMAT("形が正しくありません。"),
    UNKNOWN_FIELD("知らない項目です。");

    private final String message;

    FieldErrorCode(String message) {
        this.message = message;
    }

    /** 機能が文を決めていないときに使う文 */
    public String message() {
        return message;
    }
}
