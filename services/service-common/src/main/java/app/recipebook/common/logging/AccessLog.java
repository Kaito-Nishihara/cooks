package app.recipebook.common.logging;

import static io.quarkiverse.loggingjson.providers.KeyValueStructuredArgument.kv;

import app.recipebook.common.http.RequestAttributes;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

/**
 * 要求の記録(「共通設計: サービスの共通構成」の 4.6)。
 * 要求を 1 つ処理し終えるたびに、1 行を出す。
 * 出してよい項目を選んで出す。ヘッダー、本文、実際のパス、クエリの変数の値は出さない。
 */
@ApplicationScoped
public class AccessLog {

    private static final Logger LOG = Logger.getLogger(AccessLog.class);

    public void record(RoutingContext context, long startedAtNanos) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
        String errorCode = context.get(RequestAttributes.ERROR_CODE);

        List<Object> fields = new ArrayList<>();
        fields.add(kv("method", context.request().method().name()));
        // パスの型を出す。どの入口にも当たらなかったときは null になる
        fields.add(kv("route", context.<String>get(RequestAttributes.ROUTE)));
        fields.add(kv("status", context.response().getStatusCode()));
        fields.add(kv("durationMs", durationMs));
        if (errorCode != null) {
            fields.add(kv("errorCode", errorCode));
        }

        // 応答を返し終えた後は、要求を受けたときと別の場所で動くことがある。識別子を付け直し、終わったら戻す
        Object previous = MDC.get(RequestAttributes.REQUEST_ID);
        MDC.put(RequestAttributes.REQUEST_ID, context.<String>get(RequestAttributes.REQUEST_ID));
        try {
            LOG.infof("要求を処理しました。", fields.toArray());
        } finally {
            if (previous == null) {
                MDC.remove(RequestAttributes.REQUEST_ID);
            } else {
                MDC.put(RequestAttributes.REQUEST_ID, previous);
            }
        }
    }
}
