package app.recipebook.auth.token.presentation;

import app.recipebook.auth.token.application.PublicKeySet;
import app.recipebook.common.security.PublicEndpoint;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * 署名を確かめるための公開の鍵を返す(「共通設計: 認証」の 4.8)。
 * ほかのサービスだけが呼ぶ。入口からは届かない。
 * 形は、鍵の一覧を表す決まった形(JWKS)にする。トークンを確かめる部品が、この形を読むためである。
 */
@PublicEndpoint
@Path("/internal/auth/keys")
public class KeysResource {

    @Inject
    PublicKeySet publicKeySet;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public KeySetResponse keys() {
        return KeySetResponse.from(publicKeySet.publish());
    }
}
