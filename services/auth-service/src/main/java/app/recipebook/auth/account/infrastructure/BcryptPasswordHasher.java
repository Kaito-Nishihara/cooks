package app.recipebook.auth.account.infrastructure;

import app.recipebook.auth.account.domain.PasswordHash;
import app.recipebook.auth.account.domain.PasswordHasher;
import app.recipebook.auth.account.domain.RawPassword;
import app.recipebook.auth.config.AuthSettings;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.security.SecureRandom;
import java.util.Base64;

/** bcrypt でパスワードをハッシュにする(「共通設計: 認証」の 4.2)。 */
@ApplicationScoped
public class BcryptPasswordHasher implements PasswordHasher {

    private final int cost;

    /** 比べる相手がいないときに使う、誰のものでもないハッシュ */
    private final String placeholderHash;

    @Inject
    public BcryptPasswordHasher(AuthSettings settings) {
        this(settings.bcryptCost());
    }

    BcryptPasswordHasher(int cost) {
        this.cost = cost;
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        this.placeholderHash = BcryptUtil.bcryptHash(Base64.getEncoder().encodeToString(random), cost);
    }

    @Override
    public PasswordHash hash(RawPassword password) {
        return new PasswordHash(BcryptUtil.bcryptHash(password.reveal(), cost));
    }

    @Override
    public boolean matches(RawPassword password, PasswordHash hash) {
        return BcryptUtil.matches(password.reveal(), hash.value());
    }

    @Override
    public void burn(RawPassword password) {
        BcryptUtil.matches(password.reveal(), placeholderHash);
    }
}
