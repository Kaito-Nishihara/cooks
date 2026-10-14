package app.recipebook.auth.shared.presentation;

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
     * 決まりは、要求を処理するときに取る。
     * このクラスは、設定を読み終える前(アプリを組み立てる段階)に作られるので、作るときには取れない。
     */
    @Inject
    Instance<OriginPolicy> originPolicy;

    @RequireSameOrigin
    @ServerRequestFilter(priority = Priorities.AUTHENTICATION)
    public void check(ContainerRequestContext request) {
        originPolicy.get().requireSameOrigin(request.getHeaderString("Origin"));
    }
}
