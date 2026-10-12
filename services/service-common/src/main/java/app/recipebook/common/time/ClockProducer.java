package app.recipebook.common.time;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Clock;

/**
 * 今の時刻を取るための時計を用意する。
 * ユースケースは、この時計から時刻を取り、ドメインへ引数で渡す。Instant.now() を直接呼ばない。
 * テストは、別の時計に差し替えて、時刻を固定する。
 */
public class ClockProducer {

    @Produces
    @Singleton
    @DefaultBean
    Clock clock() {
        return Clock.systemUTC();
    }
}
