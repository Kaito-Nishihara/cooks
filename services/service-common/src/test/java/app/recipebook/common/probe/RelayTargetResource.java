package app.recipebook.common.probe;

import app.recipebook.common.security.PublicEndpoint;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** ほかのサービスの代役。届いた要求のヘッダーを覚えておく。 */
@PublicEndpoint
@Path("/internal/relay-target")
@Produces(MediaType.APPLICATION_JSON)
public class RelayTargetResource {

    private static final AtomicReference<Map<String, String>> LAST_HEADERS = new AtomicReference<>(Map.of());

    @GET
    public Map<String, String> echo(
            @HeaderParam("X-Request-Id") String requestId,
            @HeaderParam("Authorization") String authorization) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Request-Id", requestId);
        headers.put("Authorization", authorization);
        LAST_HEADERS.set(headers);
        return Map.of("status", "ok");
    }

    /** 最後に届いた要求のヘッダーを返す。 */
    public static Map<String, String> lastHeaders() {
        return LAST_HEADERS.get();
    }
}
