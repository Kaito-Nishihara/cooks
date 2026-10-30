package app.recipebook.common.error;

import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * 例外をエラーの応答に変える、ただ 1 つの場所(「共通設計: サービスの共通構成」の 4.4)。
 * 入口、ユースケース、ドメインは、応答を自分で組み立てずに例外を投げる。
 */
public class ErrorMappers {

    private static final Logger LOG = Logger.getLogger(ErrorMappers.class);

    @Inject
    ErrorWriter errorWriter;

    /** ドメインかユースケースが投げた例外。種類から、状態と文が決まる。 */
    @ServerExceptionMapper
    public Response appException(AppException exception, RoutingContext context) {
        return errorWriter.toResponse(context, exception.code(), exception.details());
    }

    /** 入力が Bean Validation の決まりに合わない。違反をすべて返す。 */
    @ServerExceptionMapper
    public Response violation(ConstraintViolationException exception, RoutingContext context) {
        return validationFailed(context, ViolationTranslator.translate(exception.getConstraintViolations()));
    }

    /** 本文が JSON として読めない。知らない項目がある。型が合わない。 */
    @ServerExceptionMapper({JsonProcessingException.class, MismatchedInputException.class})
    public Response unreadableJson(JsonProcessingException exception, RoutingContext context) {
        return validationFailed(context, JsonErrorTranslator.translate(exception));
    }

    /** 枠組みが投げた例外。パスがない、メソッドが違う、Content-Type が違う、など。 */
    @ServerExceptionMapper
    public Response webApplication(WebApplicationException exception, RoutingContext context) {
        JsonProcessingException json = JsonErrorTranslator.findIn(exception);
        if (json != null) {
            return validationFailed(context, JsonErrorTranslator.translate(json));
        }
        ErrorCode code = codeOf(exception.getResponse().getStatus());
        if (code == ErrorCode.VALIDATION_FAILED) {
            return validationFailed(context, List.of());
        }
        if (code == ErrorCode.INTERNAL_ERROR) {
            return unexpected(exception, context);
        }
        return errorWriter.toResponse(context, code, Map.of());
    }

    /** トークンがない、署名や期限が合わない。理由は応答に出さない。 */
    @ServerExceptionMapper({AuthenticationFailedException.class, UnauthorizedException.class})
    public Response unauthenticated(RuntimeException exception, RoutingContext context) {
        return errorWriter.toResponse(context, ErrorCode.UNAUTHENTICATED, Map.of());
    }

    /** 上のどれでもない例外。原因はログにだけ出し、応答には出さない。 */
    @ServerExceptionMapper
    public Response unexpected(Throwable exception, RoutingContext context) {
        LOG.error("想定していない例外が起きました。", exception);
        return errorWriter.toResponse(context, ErrorCode.INTERNAL_ERROR, Map.of());
    }

    private Response validationFailed(RoutingContext context, List<FieldError> fields) {
        return errorWriter.toResponse(context, ErrorCode.VALIDATION_FAILED, Map.of("fields", fields));
    }

    /** 枠組みが決めた状態を、このアプリのエラーの種類に置き換える。 */
    private static ErrorCode codeOf(int status) {
        return switch (status) {
            case 400, 406, 415 -> ErrorCode.VALIDATION_FAILED;
            case 401 -> ErrorCode.UNAUTHENTICATED;
            case 403 -> ErrorCode.FORBIDDEN;
            // メソッドが違うときも、パスがないときと同じに返す
            case 404, 405 -> ErrorCode.NOT_FOUND;
            case 413 -> ErrorCode.PAYLOAD_TOO_LARGE;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
