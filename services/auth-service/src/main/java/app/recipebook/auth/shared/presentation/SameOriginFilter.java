package app.recipebook.auth.shared.presentation;

import app.recipebook.auth.config.AuthSettings;
import app.recipebook.common.core.AppException;
import app.recipebook.common.core.ErrorCode;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * 要求の Origin ヘッダーが、自分のサイトと一致することを確かめる。
 * 一致しないか、ヘッダーがないときは拒む。入力の検証より前に動く。
 */
public class SameOriginFilter {

    /**
     * 設定は、要求を処理するときに取る。
     * このクラスは、設定を読み終える前(アプリを組み立てる段階)に作られるので、作るときには取れない。
     */
    @Inject
    Instance<AuthSettings> settings;

    @RequireSameOrigin
    @ServerRequestFilter(priority = Priorities.AUTHENTICATION)
    public void check(ContainerRequestContext request) {
        String origin = request.getHeaderString("Origin");
        if (origin == null || !origin.equals(settings.get().webOrigin())) {
            throw new AppException(ErrorCode.CROSS_SITE_REQUEST);
        }
    }
}
