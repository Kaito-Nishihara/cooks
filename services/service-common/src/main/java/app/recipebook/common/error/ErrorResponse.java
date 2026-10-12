package app.recipebook.common.error;

import app.recipebook.common.core.ErrorCode;
import java.util.Map;

/**
 * エラーの応答の本文(「共通設計: API の共通仕様」の 4.8)。
 * どのサービスも、この形で返す。
 *
 * @param code エラーの種類
 * @param message 利用者に見せてよい文
 * @param details 種類ごとの追加の情報。ないときは null
 */
public record ErrorResponse(String code, String message, Map<String, Object> details) {

    public static ErrorResponse of(ErrorCode code, Map<String, Object> details) {
        boolean hasDetails = details != null && !details.isEmpty();
        return new ErrorResponse(code.name(), code.message(), hasDetails ? details : null);
    }
}
