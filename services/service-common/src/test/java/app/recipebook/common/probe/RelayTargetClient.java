package app.recipebook.common.probe;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** 代役を呼ぶためのクライアント。 */
@RegisterRestClient(configKey = "relay-target")
@Path("/internal/relay-target")
public interface RelayTargetClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, String> echo();
}
