package app.recipebook.auth.session.presentation;

import app.recipebook.auth.account.presentation.UserResponse;
import app.recipebook.auth.session.application.LoginCommand;
import app.recipebook.auth.session.application.LoginUseCase;
import app.recipebook.auth.session.application.SignedIn;
import app.recipebook.auth.shared.presentation.RequireSameOrigin;
import app.recipebook.auth.shared.presentation.SessionCookie;
import app.recipebook.common.security.PublicEndpoint;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;

/** ログインの入口(「機能設計: 登録とログイン」の 4.4)。 */
@PublicEndpoint
@Path("/api/auth/login")
@Produces(MediaType.APPLICATION_JSON)
public class LoginResource {

    @Inject
    LoginUseCase loginUseCase;

    @Operation(operationId = "loginUser")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequireSameOrigin
    public Response login(@Valid @NotNull LoginRequest request,
            @CookieParam(SessionCookie.NAME) String sessionCookie) {
        SignedIn signedIn = loginUseCase.execute(new LoginCommand(
                request.email(), request.password(), SessionCookie.read(sessionCookie)));
        return Response.ok(UserResponse.from(signedIn.user()))
                .cookie(SessionCookie.issue(signedIn.token()))
                .build();
    }
}
