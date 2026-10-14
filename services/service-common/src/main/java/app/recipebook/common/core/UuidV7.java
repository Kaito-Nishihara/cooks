package app.recipebook.common.core;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * UUID のバージョン 7 を作る、ただ 1 つの場所(「共通設計: API の共通仕様」の 4.4)。
 * 先頭の 48 ビットが作った時刻(ミリ秒)なので、作った順に並ぶ。残りは乱数である。
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    /** 渡した時刻で作る。時刻は、ユースケースが時計から取って渡す。 */
    public static UUID at(Instant now) {
        long millis = now.toEpochMilli();
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);

        // 上位: 時刻の 48 ビット、版の 4 ビット(7)、乱数の 12 ビット
        long most = (millis << 16) | 0x7000L | (((random[0] & 0x0FL) << 8) | (random[1] & 0xFFL));
        // 下位: 種類の 2 ビット(10)、乱数の 62 ビット
        long least = 0L;
        for (int i = 2; i < 10; i++) {
            least = (least << 8) | (random[i] & 0xFFL);
        }
        least = (least & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(most, least);
    }
}
