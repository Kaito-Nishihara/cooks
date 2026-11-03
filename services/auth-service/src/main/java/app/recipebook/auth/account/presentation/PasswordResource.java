package app.recipebook.auth.account.presentation;

import app.recipebook.auth.account.application.ChangePasswordCommand;
import app.recipebook.auth.account.application.ChangePasswordUseCase;
import app.recipebook.auth.shared.presentation.CurrentUserId;
import app.recipebook.auth.shared.presentation.SessionCookie;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;

/**
 * パスワードの変更の入口(「機能設計: 登録とログイン」の 4.7)。
 * 入口が付けたトークンと、ブラウザが送ったクッキーの両方を受け取る。クッキーは変えない。
 */
@Path("/api/me/password")
@Produces(MediaType.APPLICATION_JSON)
public class PasswordResource {

    @Inject
    ChangePasswordUseCase changePasswordUseCase;

    @Inject
    CurrentUserId currentUserId;

    @Operation(operationId = "changePassword")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public UserResponse change(@Valid @NotNull ChangePasswordRequest request,
            @CookieParam(SessionCookie.NAME) String sessionCookie) {
        return UserResponse.from(changePasswordUseCase.execute(new ChangePasswordCommand(
                currentUserId.get(), SessionCookie.read(sessionCookie),
                request.currentPassword(), request.newPassword())));
    }
}
