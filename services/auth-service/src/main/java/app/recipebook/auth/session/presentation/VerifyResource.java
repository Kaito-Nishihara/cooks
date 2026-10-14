package app.recipebook.auth.session.presentation;

import app.recipebook.auth.session.application.VerifySessionUseCase;
import app.recipebook.auth.shared.presentation.OriginPolicy;
import app.recipebook.auth.shared.presentation.SessionCookie;
import app.recipebook.common.security.PublicEndpoint;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/**
 * 入口(Envoy Gateway)が、ログインが要る要求を後ろへ渡す前に呼ぶ(「共通設計: 認証」の 4.6)。
 * 入口は、元の要求と同じメソッドで、/internal/auth/verify の後ろに元のパスを付けて呼ぶ。
 * 届いた要求のメソッドを、元のメソッドとして扱う。後ろに付いたパスは使わない。
 * 入口から届かない場所(/internal)に置く。
 */
@PublicEndpoint
@Path("/internal/auth/verify")
public class VerifyResource {

    @Inject
    VerifySessionUseCase verifySessionUseCase;

    @Inject
    OriginPolicy originPolicy;

    @GET
    @Path("{originalPath: .*}")
    public Response get(@CookieParam(SessionCookie.NAME) String cookie, @HeaderParam("Origin") String origin) {
        return verify("GET", cookie, origin);
    }

    @HEAD
    @Path("{originalPath: .*}")
    public Response head(@CookieParam(SessionCookie.NAME) String cookie, @HeaderParam("Origin") String origin) {
        return verify("HEAD", cookie, origin);
    }

    @OPTIONS
    @Path("{originalPath: .*}")
    public Response options(@CookieParam(SessionCookie.NAME) String cookie, @HeaderParam("Origin") String origin) {
        return verify("OPTIONS", cookie, origin);
    }

    @POST
    @Path("{originalPath: .*}")
    public Response post(@CookieParam(SessionCookie.NAME) String cookie, @HeaderParam("Origin") String origin) {
        return verify("POST", cookie, origin);
    }

    @PUT
    @Path("{originalPath: .*}")
    public Response put(@CookieParam(SessionCookie.NAME) String cookie, @HeaderParam("Origin") String origin) {
        return verify("PUT", cookie, origin);
    }

    @PATCH
    @Path("{originalPath: .*}")
    public Response patch(@CookieParam(SessionCookie.NAME) String cookie, @HeaderParam("Origin") String origin) {
        return verify("PATCH", cookie, origin);
    }

    @DELETE
    @Path("{originalPath: .*}")
    public Response delete(@CookieParam(SessionCookie.NAME) String cookie, @HeaderParam("Origin") String origin) {
        return verify("DELETE", cookie, origin);
    }

    /** 送り元を確かめ、セッションを確かめ、トークンを Authorization ヘッダーに入れて返す。本文は返さない。 */
    private Response verify(String method, String cookie, String origin) {
        originPolicy.requireSameOriginIfStateChanging(method, origin);
        String token = verifySessionUseCase.execute(SessionCookie.read(cookie));
        return Response.ok().header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
    }
}
