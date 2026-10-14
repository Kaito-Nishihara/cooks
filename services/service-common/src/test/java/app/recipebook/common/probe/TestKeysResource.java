package app.recipebook.common.probe;

import app.recipebook.common.security.PublicEndpoint;
import app.recipebook.common.testing.TestTokens;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** auth-service の公開の鍵の入口の代役。テスト用の鍵を配る。 */
@PublicEndpoint
@Path("/internal/auth/keys")
public class TestKeysResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String keys() {
        return TestTokens.jwks();
    }
}
