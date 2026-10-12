package app.recipebook.common.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ドメインとユースケースが投げる、ただ 1 種類の例外。
 * エラーの種類と、応答の details に入れる値だけを持つ。
 * HTTP の状態と文は、種類から決まるので、投げる側は指定しない。
 */
public class AppException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> details;

    public AppException(ErrorCode code) {
        this(code, Map.of());
    }

    public AppException(ErrorCode code, Map<String, Object> details) {
        this(code, details, null);
    }

    /** もとの例外を残して投げる。もとの例外は、応答に出ない。 */
    public AppException(ErrorCode code, Map<String, Object> details, Throwable cause) {
        super(Objects.requireNonNull(code, "code").name(), cause);
        this.code = code;
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(details, "details")));
    }

    public ErrorCode code() {
        return code;
    }

    /** details に入れる値。ないときは空 */
    public Map<String, Object> details() {
        return details;
    }
}
