package app.recipebook.common.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 必要な設定が欠けているときに、起動を失敗させる(「共通設計: サービスの共通構成」の 4.7)。
 * 動き出してから気付くのを防ぐためである。
 * 確かめる設定の名前は、サービスが app.required-settings に並べる。
 */
@ApplicationScoped
public class RequiredSettings {

    @ConfigProperty(name = "app.required-settings")
    Optional<List<String>> names;

    @Inject
    Config config;

    void check(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) StartupEvent event) {
        List<String> missing = names.orElse(List.of()).stream()
                .filter(name -> config.getOptionalValue(name, String.class).filter(value -> !value.isBlank()).isEmpty())
                .toList();
        if (!missing.isEmpty()) {
            // 設定の名前だけを出す。値は出さない
            throw new IllegalStateException("必要な設定がありません: " + String.join(", ", missing));
        }
    }
}
