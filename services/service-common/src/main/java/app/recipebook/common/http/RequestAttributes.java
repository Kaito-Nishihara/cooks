package app.recipebook.common.http;

/** 1 つの要求を処理する間に、部品の間で受け渡す値の名前。 */
public final class RequestAttributes {

    /** 要求の識別子を運ぶヘッダー */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    /** ログの行に付ける、要求の識別子の項目の名前 */
    public static final String REQUEST_ID = "requestId";
    /** パスの型。例: /api/books/{bookId} */
    public static final String ROUTE = "app.route";
    /** 返したエラーの種類 */
    public static final String ERROR_CODE = "app.errorCode";

    private RequestAttributes() {
    }
}
