package app.recipebook.common.http;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** 要求の大きさの上限の設定。名前は app.http で始まる。 */
@ConfigMapping(prefix = "app.http")
public interface HttpLimits {

    /** 要求の本文の上限(バイト)。既定は 64KB */
    @WithDefault("65536")
    long maxBodyBytes();
}
