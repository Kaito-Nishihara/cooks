package app.recipebook.common.probe;

import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * 共通のライブラリの動きを確かめるための、テストだけの入口。
 * 本物のサービスの入口の代わりに、決まった応答と例外を返す。
 */
@Path("/api/probes")
@Produces(MediaType.APPLICATION_JSON)
public class ProbeResource {

    /** 想定していない例外の文言。応答に漏れないことを確かめるために使う */
    public static final String CRASH_DETAIL = "probe_table の行が壊れている";

    @Inject
    @RestClient
    RelayTargetClient relayTarget;

    /** 入力の形 */
    public record ProbeRequest(
            @NotBlank @Size(max = 20) String name,
            @NotBlank String email,
            String password,
            @Valid @Size(max = 3) List<Row> rows) {
    }

    /** 入力の中の、配列の 1 件 */
    public record Row(@NotNull @Min(1) Integer amount) {
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid @NotNull ProbeRequest request) {
        return Response.status(Response.Status.CREATED).entity(Map.of("name", request.name())).build();
    }

    @GET
    @Path("/{probeId}")
    public Map<String, String> get(@PathParam("probeId") String probeId) {
        return Map.of("id", probeId);
    }

    @GET
    @Path("/{probeId}/conflict")
    public Map<String, String> conflict(@PathParam("probeId") String probeId) {
        throw new AppException(ErrorCode.REVISION_CONFLICT, Map.of("currentRevision", 3));
    }

    @GET
    @Path("/{probeId}/crash")
    public Map<String, String> crash(@PathParam("probeId") String probeId) {
        throw new IllegalStateException(CRASH_DETAIL);
    }

    /** ほかのサービス(代役)を呼ぶ。 */
    @GET
    @Path("/{probeId}/relay")
    public Map<String, String> relay(@PathParam("probeId") String probeId) {
        return relayTarget.echo();
    }
}
