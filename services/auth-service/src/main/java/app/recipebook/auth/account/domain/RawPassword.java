package app.recipebook.auth.account.domain;

import app.recipebook.auth.shared.domain.InputProblem;
import app.recipebook.auth.shared.domain.UnicodeText;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Optional;

/**
 * 入力されたパスワード(「機能設計: 登録とログイン」の 4.1)。
 * NFKC で正規化した、ハッシュにする前の値を持つ。
 * 保存もログ出力もしない。文字列に直しても、中身を出さない。
 */
public final class RawPassword {

    /** 新しく設定するパスワードの、長さの下限(文字) */
    public static final int MIN_LENGTH = 12;
    /** bcrypt が読む長さの上限(UTF-8 のバイト)。超えた分は無視されるので、最初から受け付けない */
    public static final int MAX_BYTES = 72;

    private final String value;

    private RawPassword(String value) {
        this.value = value;
    }

    /** 新しく設定するパスワードを作る。登録と、パスワードの変更の新しいほうに使う。下限を確かめる。 */
    public static RawPassword forNew(String input) {
        return create(input, true);
    }

    /**
     * 今のパスワードと比べるための値を作る。ログインと、パスワードの変更の今のほうに使う。
     * 下限を確かめない。下限を後から変えたときに、変える前に登録した人が締め出されないようにするためである。
     */
    public static RawPassword forMatching(String input) {
        return create(input, false);
    }

    /** 入力が決まりに合うかを確かめる。合わないときは、最初に当てはまる理由を 1 つ返す。 */
    public static Optional<InputProblem> check(String input, boolean isNew) {
        if (input == null || input.isEmpty()) {
            return Optional.of(InputProblem.REQUIRED);
        }
        String normalized = normalize(input);
        // NUL を文字列の終わりとして扱う bcrypt の実装があるので、制御文字を拒む
        if (UnicodeText.hasControlCharacter(normalized)) {
            return Optional.of(InputProblem.INVALID_FORMAT);
        }
        if (isNew && UnicodeText.length(normalized) < MIN_LENGTH) {
            return Optional.of(InputProblem.TOO_SHORT);
        }
        if (normalized.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return Optional.of(InputProblem.TOO_LONG);
        }
        return Optional.empty();
    }

    /** ハッシュにする処理へ渡す値。ほかの用途に使わない。 */
    public String reveal() {
        return value;
    }

    @Override
    public String toString() {
        return "RawPassword[隠した値]";
    }

    private static RawPassword create(String input, boolean isNew) {
        if (check(input, isNew).isPresent()) {
            throw new IllegalArgumentException("パスワードの決まりに合わない値である");
        }
        return new RawPassword(normalize(input));
    }

    /** 端末や入力の方式による並びの違い(全角と半角など)を、そろえる。前後の空白は取らない。 */
    private static String normalize(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFKC);
    }
}
