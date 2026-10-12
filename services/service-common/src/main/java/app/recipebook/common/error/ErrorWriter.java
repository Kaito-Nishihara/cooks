package app.recipebook.common.error;

import app.recipebook.common.core.ErrorCode;
import app.recipebook.common.http.RequestAttributes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * エラーの応答を組み立てる、ただ 1 つの場所。
 * 返した種類を覚えておき、要求の記録に出せるようにする。
 */
@ApplicationScoped
public class ErrorWriter {

    private static final String JSON_UTF8 = MediaType.APPLICATION_JSON + ";charset=UTF-8";

    @Inject
    ObjectMapper objectMapper;

    /** 入口(JAX-RS)の応答として組み立てる。 */
    public Response toResponse(RoutingContext context, ErrorCode code, Map<String, Object> details) {
        context.put(RequestAttributes.ERROR_CODE, code.name());
        return Response.status(code.status())
                .type(JSON_UTF8)
                .entity(ErrorResponse.of(code, details))
                .build();
    }

    /** 入口に届く前の層で、応答を直接書く。 */
    public void write(RoutingContext context, ErrorCode code) {
        context.put(RequestAttributes.ERROR_CODE, code.name());
        context.response()
                .setStatusCode(code.status())
                .putHeader(HttpHeaders.CONTENT_TYPE, JSON_UTF8)
                .end(toJson(ErrorResponse.of(code, null)));
    }

    private String toJson(ErrorResponse body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("エラーの応答を JSON にできない", e);
        }
    }
}
