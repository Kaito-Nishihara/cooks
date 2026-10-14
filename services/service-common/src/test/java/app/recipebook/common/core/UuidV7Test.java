package app.recipebook.common.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** UUID のバージョン 7 を作る処理(「共通設計: API の共通仕様」の 4.4)。 */
class UuidV7Test {

    private static final Instant NOW = Instant.parse("2026-10-10T03:00:00Z");

    @Test
    @DisplayName("版が 7 で、種類が RFC の決めた値である")
    void hasVersionSeven() {
        UUID id = UuidV7.at(NOW);

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    @DisplayName("先頭の 48 ビットが、渡した時刻のミリ秒である")
    void startsWithTimestamp() {
        UUID id = UuidV7.at(NOW);

        assertThat(id.getMostSignificantBits() >>> 16).isEqualTo(NOW.toEpochMilli());
    }

    @Test
    @DisplayName("後の時刻で作ったものが、文字列で比べても後ろに並ぶ")
    void sortsByCreationTime() {
        String earlier = UuidV7.at(NOW).toString();
        String later = UuidV7.at(NOW.plusMillis(1)).toString();

        assertThat(earlier.compareTo(later)).isNegative();
    }

    @Test
    @DisplayName("同じ時刻で 1,000 個作っても、重ならない")
    void isUniqueWithinSameMillisecond() {
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(UuidV7.at(NOW));
        }
        assertThat(ids).hasSize(1000);
    }
}
