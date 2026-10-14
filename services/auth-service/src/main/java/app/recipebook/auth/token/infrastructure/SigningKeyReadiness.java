package app.recipebook.auth.token.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * 署名の鍵を読めているときだけ、要求を受けられると答える(「共通設計: サービスの共通構成」の 4.8)。
 * 鍵がないと、ログインしている人のどの要求も通せないためである。
 */
@Readiness
@ApplicationScoped
public class SigningKeyReadiness implements HealthCheck {

    @Inject
    SigningKeys signingKeys;

    @Override
    public HealthCheckResponse call() {
        // 応答に、鍵の名前や設定の値を入れない
        return HealthCheckResponse.named("signing-key").status(signingKeys.isReady()).build();
    }
}
