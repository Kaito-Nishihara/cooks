package app.recipebook.common.core;

/**
 * エラーの種類の一覧(「共通設計: API の共通仕様」の 4.9)。
 * HTTP の状態と、利用者に見せる文は、ここから決まる。
 * 種類を足すときは、先に仕様書の表に足す。
 */
public enum ErrorCode {

    VALIDATION_FAILED(400, "入力に誤りがあります。"),
    UNAUTHENTICATED(401, "ログインが必要です。"),
    LOGIN_FAILED(401, "メールアドレスかパスワードが違います。"),
    FORBIDDEN(403, "この操作をする権限がありません。"),
    CROSS_SITE_REQUEST(403, "この画面からは操作できません。"),
    NOT_FOUND(404, "見つかりませんでした。"),
    EMAIL_TAKEN(409, "このメールアドレスは、すでに登録されています。"),
    REVISION_CONFLICT(409, "ほかの人が先に保存しました。"),
    LIMIT_EXCEEDED(409, "上限に達しています。"),
    INVITATION_INVALID(410, "この招待は使えません。"),
    PAYLOAD_TOO_LARGE(413, "送る内容が大きすぎます。"),
    LOGIN_LOCKED(429, "ログインの失敗が続いたため、しばらく受け付けません。"),
    INTERNAL_ERROR(500, "サーバーで問題が起きました。時間をおいて、もう一度お試しください。"),
    DEPENDENCY_UNAVAILABLE(503, "いまは処理できません。時間をおいて、もう一度お試しください。");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    /** HTTP の状態 */
    public int status() {
        return status;
    }

    /** 利用者に見せてよい文。内部の事情を含めない */
    public String message() {
        return message;
    }
}
