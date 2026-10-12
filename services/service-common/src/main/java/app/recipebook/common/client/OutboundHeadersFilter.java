package app.recipebook.common.client;

import app.recipebook.common.http.RequestAttributes;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

/**
 * ほかのサービスを呼ぶときに、受け取ったトークンと要求の識別子を付けて渡す
 * (「共通設計: サービスの共通構成」の 6.2)。
 * どのクライアントにも自動で付くので、呼び出しの実装ごとに書かない。
 */
@Provider
public class OutboundHeadersFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext outbound) {
        RoutingContext inbound = currentRequest();
        if (inbound == null) {
            // 要求の外(定期の処理など)からの呼び出しには、渡すものがない
            return;
        }
        copy(outbound, RequestAttributes.REQUEST_ID_HEADER, inbound.get(RequestAttributes.REQUEST_ID));
        copy(outbound, HttpHeaders.AUTHORIZATION, inbound.request().getHeader(HttpHeaders.AUTHORIZATION));
    }

    private static void copy(ClientRequestContext outbound, String name, String value) {
        if (value != null && !outbound.getHeaders().containsKey(name)) {
            outbound.getHeaders().putSingle(name, value);
        }
    }

    /** いま処理している要求を返す。要求の外では null を返す。 */
    private static RoutingContext currentRequest() {
        ArcContainer container = Arc.container();
        if (container == null || !container.requestContext().isActive()) {
            return null;
        }
        return container.instance(CurrentVertxRequest.class).get().getCurrent();
    }
}
