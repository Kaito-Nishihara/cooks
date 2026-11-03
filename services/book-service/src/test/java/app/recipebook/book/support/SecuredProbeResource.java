package app.recipebook.book.support;

import app.recipebook.common.security.CurrentUser;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/**
 * トークンが要る、テストだけの入口。PublicEndpoint の印を付けない。
 * 骨組みの段階では、トークンが要る本物の入口がまだないので、これでトークンの確かめ方を確かめる。
 */
@Path("/api/secured-probes")
@Produces(MediaType.APPLICATION_JSON)
public class SecuredProbeResource {

    @Inject
    CurrentUser currentUser;

    @GET
    public Map<String, String> whoAmI() {
        return Map.of("userId", currentUser.id().toString());
    }
}
