package app.recipebook.common.http;

import app.recipebook.common.core.ErrorCode;
import app.recipebook.common.error.ErrorWriter;
import app.recipebook.common.logging.AccessLog;
import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.MDC;

/**
 * 要求を受けた直後と、応答を返し終えた後の処理。
 * どの入口にも当てはまるように、個々の入口より手前(HTTP のサーバーの層)に置く。
 * ないパスへの要求にも、要求の識別子と要求の記録を付けるためである。
 * 稼働の確認は別のポートで受けるので、ここを通らない。
 */
@ApplicationScoped
public class RequestLifecycle {

    /** ほかのフィルターより先に動かす。大きいほど先に動く */
    private static final int FIRST = 10_000;

    @Inject
    AccessLog accessLog;

    @Inject
    ErrorWriter errorWriter;

    void registerFilter(@Observes Filters filters) {
        filters.register(this::begin, FIRST);
    }

    /** どの入口にも当たらなかった要求に、決まった形の 404 を返す。 */
    void registerFallback(@Observes Router router) {
        router.route().last().handler(context -> errorWriter.write(context, ErrorCode.NOT_FOUND));
    }

    private void begin(RoutingContext context) {
        String requestId = RequestId.resolve(context.request().getHeader(RequestAttributes.REQUEST_ID_HEADER));
        context.put(RequestAttributes.REQUEST_ID, requestId);
        context.response().putHeader(RequestAttributes.REQUEST_ID_HEADER, requestId);
        // この要求を処理する間のすべてのログの行に、識別子を付ける
        MDC.put(RequestAttributes.REQUEST_ID, requestId);

        long startedAt = System.nanoTime();
        context.addEndHandler(result -> accessLog.record(context, startedAt));
        context.next();
    }
}
