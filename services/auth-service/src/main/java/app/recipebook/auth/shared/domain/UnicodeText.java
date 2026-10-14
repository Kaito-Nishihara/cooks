package app.recipebook.auth.shared.domain;

/**
 * 文字列を、Unicode の符号位置で扱うための道具(「機能設計: 登録とログイン」の 4.1)。
 * String.length() は「𠮷」を 2 と数えるので、長さを数えるときに使わない。
 */
public final class UnicodeText {

    private UnicodeText() {
    }

    /** 文字の数を、符号位置で数える。 */
    public static int length(String text) {
        return text.codePointCount(0, text.length());
    }

    /** 前後の空白(Unicode が White_Space と定めている文字)を取る。 */
    public static String trim(String text) {
        int start = 0;
        int end = text.length();
        while (start < end) {
            int codePoint = text.codePointAt(start);
            if (!isWhiteSpace(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        while (end > start) {
            int codePoint = text.codePointBefore(end);
            if (!isWhiteSpace(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return text.substring(start, end);
    }

    /** 制御文字(改行、タブ、NUL など)を含むかどうかを返す。 */
    public static boolean hasControlCharacter(String text) {
        return text.codePoints().anyMatch(codePoint -> Character.getType(codePoint) == Character.CONTROL);
    }

    /**
     * Unicode の White_Space の性質を持つ文字かどうかを返す。
     * Character.isWhitespace は、切れ目のない空白(U+00A0 など)を含まないので使わない。
     */
    static boolean isWhiteSpace(int codePoint) {
        return (codePoint >= 0x0009 && codePoint <= 0x000D)
                || codePoint == 0x0020
                || codePoint == 0x0085
                || codePoint == 0x00A0
                || codePoint == 0x1680
                || (codePoint >= 0x2000 && codePoint <= 0x200A)
                || codePoint == 0x2028
                || codePoint == 0x2029
                || codePoint == 0x202F
                || codePoint == 0x205F
                || codePoint == 0x3000;
    }
}
