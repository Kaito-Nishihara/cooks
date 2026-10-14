package app.recipebook.auth.account.domain;

import app.recipebook.auth.shared.domain.InputProblem;
import app.recipebook.auth.shared.domain.UnicodeText;
import java.text.Normalizer;
import java.util.Optional;

/**
 * 表示名(「機能設計: 登録とログイン」の 4.1)。
 * NFC で正規化し、前後の空白を取った値を持つ。
 */
public record DisplayName(String value) {

    public static final int MAX_LENGTH = 50;

    public DisplayName {
        if (check(value).isPresent() || !value.equals(normalize(value))) {
            throw new IllegalArgumentException("表示名の決まりに合わない値である");
        }
    }

    /** 入力から作る。正規化と、前後の空白を取る処理を行う。 */
    public static DisplayName of(String input) {
        if (input == null) {
            throw new IllegalArgumentException("表示名がない");
        }
        return new DisplayName(normalize(input));
    }

    /** 入力が決まりに合うかを確かめる。合わないときは、最初に当てはまる理由を 1 つ返す。 */
    public static Optional<InputProblem> check(String input) {
        if (input == null) {
            return Optional.of(InputProblem.REQUIRED);
        }
        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return Optional.of(InputProblem.REQUIRED);
        }
        if (normalized.codePoints().anyMatch(DisplayName::isForbidden)) {
            return Optional.of(InputProblem.INVALID_FORMAT);
        }
        if (UnicodeText.length(normalized) > MAX_LENGTH) {
            return Optional.of(InputProblem.TOO_LONG);
        }
        return Optional.empty();
    }

    private static String normalize(String input) {
        return UnicodeText.trim(Normalizer.normalize(input, Normalizer.Form.NFC));
    }

    /**
     * 使えない文字かどうかを返す。
     * 制御文字、行と段落の区切り、文字の向きを変える指示を拒む。
     * 表示名は、ほかの人の画面に出るので、後ろに続く文の並びを変える文字を入れさせない。
     */
    private static boolean isForbidden(int codePoint) {
        return Character.getType(codePoint) == Character.CONTROL
                || codePoint == 0x2028
                || codePoint == 0x2029
                || (codePoint >= 0x202A && codePoint <= 0x202E)
                || (codePoint >= 0x2066 && codePoint <= 0x2069);
    }
}
