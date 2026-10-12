package app.recipebook.auth.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** このサービスが足す設定。名前は app.auth で始まる。環境変数では APP_AUTH_ で始まる。 */
@ConfigMapping(prefix = "app.auth")
public interface AuthSettings {

    /** 画面のオリジン。例: http://localhost:8080。状態を変える要求の送り元を、これと比べる */
    String webOrigin();

    /** bcrypt のコスト。1 上げると、計算の時間が 2 倍になる */
    @WithDefault("12")
    int bcryptCost();
}
