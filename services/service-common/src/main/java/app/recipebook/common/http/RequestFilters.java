package app.recipebook.common.http;

import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/** 入口が決まった後、入口の処理の前に、どの入口にも行うこと。 */
public class RequestFilters {

    /** 入口のメソッドから、パスの型を引く。入口の数だけしか増えない */
    private final Map<Method, String> routes = new ConcurrentHashMap<>();

    @Inject
    HttpLimits limits;

    /**
     * パスの型を覚えておく。要求の記録に、実際のパスの代わりに出すためである。
     * ログインの確認などで拒まれた要求にも付くように、ほかより先に動かす。
     */
    @ServerRequestFilter(priority = Priorities.AUTHENTICATION - 200)
    public void rememberRoute(ResourceInfo resourceInfo, RoutingContext context) {
        Method method = resourceInfo.getResourceMethod();
        if (method != null) {
            context.put(RequestAttributes.ROUTE,
                    routes.computeIfAbsent(method, m -> routeOf(resourceInfo.getResourceClass(), m)));
        }
    }

    /**
     * 本文が上限より大きい要求を、読む前に拒む(「共通設計: API の共通仕様」の 4.7)。
     * 大きさは Content-Length で確かめる。
     */
    @ServerRequestFilter(priority = Priorities.AUTHENTICATION - 100)
    public void rejectLargeBody(ContainerRequestContext request) {
        if (request.getLength() > limits.maxBodyBytes()) {
            throw new AppException(ErrorCode.PAYLOAD_TOO_LARGE);
        }
    }

    private static String routeOf(Class<?> resourceClass, Method method) {
        return segment(resourceClass.getAnnotation(Path.class)) + segment(method.getAnnotation(Path.class));
    }

    /** 前後の「/」をそろえ、変数に付いた正規表現を除く。例: "{id: [0-9]+}" は "{id}" */
    private static String segment(Path path) {
        if (path == null) {
            return "";
        }
        String value = path.value().replaceAll("\\{\\s*(\\w+)\\s*:[^}]*}", "{$1}").replaceAll("^/+|/+$", "");
        return value.isEmpty() ? "" : "/" + value;
    }
}
