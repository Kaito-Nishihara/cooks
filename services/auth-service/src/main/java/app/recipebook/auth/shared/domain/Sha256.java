package app.recipebook.auth.shared.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 のハッシュを、16 進の小文字 64 文字で表す。 */
public final class Sha256 {

    private Sha256() {
    }

    public static String hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 は、どの Java にも入っている
            throw new IllegalStateException(e);
        }
    }

    /** 64 文字の、16 進の小文字かどうかを返す。 */
    public static boolean isHex(String text) {
        return text != null && text.matches("[0-9a-f]{64}");
    }
}
