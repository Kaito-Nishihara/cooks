package app.recipebook.auth.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.List;
import java.util.Optional;

/** このサービスが足す設定。名前は app.auth で始まる。環境変数では APP_AUTH_ で始まる。 */
@ConfigMapping(prefix = "app.auth")
public interface AuthSettings {

    /** 画面のオリジン。例: http://localhost:8080。状態を変える要求の送り元を、これと比べる */
    String webOrigin();

    /** bcrypt のコスト。1 上げると、計算の時間が 2 倍になる */
    @WithDefault("12")
    int bcryptCost();

    /**
     * トークンに署名する秘密の鍵のファイル。RSA の 2,048 ビット以上で、PKCS#8 の PEM の形である。
     * Kubernetes の Secret をファイルとして載せて渡す。環境変数に鍵そのものを入れない。
     */
    String signingKeyFile();

    /**
     * 鍵を入れ替える間だけ、公開の鍵の一覧に載せ続ける、古い公開の鍵のファイル(X.509 の PEM)。
     * 古い鍵で署名したトークンが、すべて期限切れになったら外す(「共通設計: 認証」の 4.8)。
     */
    Optional<List<String>> retiredPublicKeyFiles();
}
