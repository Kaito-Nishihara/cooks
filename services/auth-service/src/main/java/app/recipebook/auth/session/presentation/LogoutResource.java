package app.recipebook.auth.session.presentation;

import app.recipebook.auth.session.application.LogoutUseCase;
import app.recipebook.auth.shared.presentation.CurrentUserId;
import app.recipebook.auth.shared.presentation.SessionCookie;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;

/**
 * ログアウトの入口(「機能設計: 登録とログイン」の 4.5)。
 * 入口が付けたトークンと、ブラウザが送ったクッキーの両方を受け取る。本文は読まない。
 */
@Path("/api/auth/logout")
public class LogoutResource {

    @Inject
    LogoutUseCase logoutUseCase;

    @Inject
    CurrentUserId currentUserId;

    @Operation(operationId = "logoutUser")
    @POST
    public Response logout(@CookieParam(SessionCookie.NAME) String sessionCookie) {
        logoutUseCase.execute(currentUserId.get(), SessionCookie.read(sessionCookie));
        return Response.noContent().cookie(SessionCookie.clear()).build();
    }
}
