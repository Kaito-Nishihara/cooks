package app.recipebook.auth.session.infrastructure;

import app.recipebook.auth.session.domain.SessionToken;
import app.recipebook.auth.session.domain.SessionTokenGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;
import java.util.Base64;

/** 256 ビットの乱数を、URL に使える文字(43 文字)にして、トークンを作る。 */
@ApplicationScoped
public class SecureRandomSessionTokenGenerator implements SessionTokenGenerator {

    private static final int BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public SessionToken generate() {
        byte[] bytes = new byte[BYTES];
        random.nextBytes(bytes);
        return new SessionToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }
}
