package app.recipebook.auth.account.presentation;

import app.recipebook.auth.account.application.GetMeUseCase;
import app.recipebook.auth.shared.presentation.CurrentUserId;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** 自分の情報の入口(「機能設計: 登録とログイン」の 4.6)。トークンが要る。 */
@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {

    @Inject
    GetMeUseCase getMeUseCase;

    @Inject
    CurrentUserId currentUserId;

    @GET
    public UserResponse getMe() {
        return UserResponse.from(getMeUseCase.execute(currentUserId.get()));
    }
}
