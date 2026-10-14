package app.recipebook.auth.token.infrastructure;

import app.recipebook.auth.shared.domain.UserId;
import app.recipebook.auth.token.application.AccessTokenIssuer;
import app.recipebook.common.core.UuidV7;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;

/** JWT を、RS256 で署名して作る(「共通設計: 認証」の 4.7)。 */
@ApplicationScoped
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    static final String ISSUER = "auth-service";
    static final String AUDIENCE = "recipe-book";

    private final SigningKeys signingKeys;

    @Inject
    public JwtAccessTokenIssuer(SigningKeys signingKeys) {
        this.signingKeys = signingKeys;
    }

    @Override
    public String issue(UserId userId, Instant now) {
        SigningKeys.Loaded key = signingKeys.current()
                .orElseThrow(() -> new IllegalStateException("署名の鍵を読めていない"));
        return Jwt.issuer(ISSUER)
                .audience(AUDIENCE)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(LIFETIME_SECONDS))
                // トークンごとに違う値。ログで要求を見分けるために使う
                .claim("jti", UuidV7.at(now).toString())
                .jws()
                .keyId(key.keyId())
                .algorithm(SignatureAlgorithm.RS256)
                .sign(key.privateKey());
    }
}
