package app.recipebook.auth.account.presentation;

import app.recipebook.auth.account.application.RegisterCommand;
import app.recipebook.auth.account.application.RegisterUseCase;
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

/** 登録の入口(「機能設計: 登録とログイン」の 4.3)。 */
@PublicEndpoint
@Path("/api/auth/register")
@Produces(MediaType.APPLICATION_JSON)
public class RegisterResource {

    @Inject
    RegisterUseCase registerUseCase;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequireSameOrigin
    public Response register(@Valid @NotNull RegisterRequest request,
            @CookieParam(SessionCookie.NAME) String sessionCookie) {
        SignedIn signedIn = registerUseCase.execute(new RegisterCommand(
                request.displayName(), request.email(), request.password(), SessionCookie.read(sessionCookie)));
        // Location は付けない。識別子を指定して利用者を取る、外向きの API がないためである
        return Response.status(Response.Status.CREATED)
                .entity(UserResponse.from(signedIn.user()))
                .cookie(SessionCookie.issue(signedIn.token()))
                .build();
    }
}
