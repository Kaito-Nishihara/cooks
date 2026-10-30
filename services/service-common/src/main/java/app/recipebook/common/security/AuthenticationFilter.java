package app.recipebook.common.security;

import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ResourceInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.MDC;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * 入口に当たった要求の本人を確かめる。
 * PublicEndpoint の印がない入口は、有効なトークンがなければ、401 と UNAUTHENTICATED で拒む。
 * 印がある入口では、トークンを確かめない。
 * 確かめた本人の識別子を、ログの行の userId に付ける。
 */
public class AuthenticationFilter {

    /** ログの行に付ける、利用者の識別子の項目の名前 */
    static final String USER_ID = "userId";

    /** このクラスは、設定を読み終える前に作られるので、使うときに取る(services/CLAUDE.md) */
    @Inject
    Instance<CurrentIdentityAssociation> identityAssociation;

    @ServerRequestFilter(priority = Priorities.AUTHENTICATION + 100)
    public Uni<Void> authenticate(ResourceInfo resourceInfo) {
        // トークンなしで呼べる入口では、トークンを確かめない。付いていても読まない
        if (resourceInfo.getResourceMethod().isAnnotationPresent(PublicEndpoint.class)
                || resourceInfo.getResourceClass().isAnnotationPresent(PublicEndpoint.class)) {
            return Uni.createFrom().voidItem();
        }
        // トークンの署名と期限を、ここで確かめる。確かめられなければ、AuthenticationFailedException になる
        return identityAssociation.get().getDeferredIdentity()
                .invoke(identity -> {
                    String userId = userIdOf(identity);
                    if (userId == null) {
                        throw new AppException(ErrorCode.UNAUTHENTICATED);
                    }
                    MDC.put(USER_ID, userId);
                })
                .replaceWithVoid();
    }

    /** 本人の識別子を返す。トークンがないか、sub が UUID の形でなければ null を返す。 */
    private static String userIdOf(SecurityIdentity identity) {
        if (identity.isAnonymous() || !(identity.getPrincipal() instanceof JsonWebToken token)) {
            return null;
        }
        var id = CurrentUser.parse(token.getSubject());
        return id == null ? null : id.toString();
    }
}
