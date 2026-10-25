package app.recipebook.auth.account.presentation;

import app.recipebook.auth.account.application.ListUsersUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;

/**
 * 表示名をまとめて返す入口(「機能設計: 登録とログイン」の 4.8)。
 * サービスだけが呼び、入口(Envoy Gateway)からは届かない。トークンが要る。
 */
@Path("/internal/users")
@Produces(MediaType.APPLICATION_JSON)
public class InternalUsersResource {

    @Inject
    ListUsersUseCase listUsersUseCase;

    @Operation(operationId = "listUsers")
    @GET
    public UserListResponse listUsers(@QueryParam("ids") @UserIdList String ids) {
        return UserListResponse.of(listUsersUseCase.execute(UserIdListText.parse(ids)));
    }
}
