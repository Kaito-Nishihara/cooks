package app.recipebook.auth.account.domain;

import app.recipebook.auth.shared.domain.InputProblem;
import app.recipebook.auth.shared.domain.Sha256;
import app.recipebook.auth.shared.domain.UnicodeText;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * メールアドレス(「機能設計: 登録とログイン」の 4.1)。
 * 前後の空白を取り、小文字にそろえた値を持つ。
 * 小文字にそろえた値が同じなら、同じメールアドレスである。
 */
public record EmailAddress(String value) {

    public static final int MAX_LENGTH = 254;

    /** HTML の仕様の形に、「@」の前の長さの上限と、「@」の後ろに「.」が要ることを足したもの */
    private static final Pattern FORMAT = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]{1,64}"
                    + "@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");

    public EmailAddress {
        if (check(value).isPresent() || !value.equals(normalize(value))) {
            throw new IllegalArgumentException("メールアドレスの決まりに合わない値である");
        }
    }

    /** 入力から作る。前後の空白を取り、小文字にそろえる。 */
    public static EmailAddress of(String input) {
        if (input == null) {
            throw new IllegalArgumentException("メールアドレスがない");
        }
        return new EmailAddress(normalize(input));
    }

    /** 入力が決まりに合うかを確かめる。合わないときは、最初に当てはまる理由を 1 つ返す。 */
    public static Optional<InputProblem> check(String input) {
        if (input == null) {
            return Optional.of(InputProblem.REQUIRED);
        }
        String trimmed = UnicodeText.trim(input);
        if (trimmed.isEmpty()) {
            return Optional.of(InputProblem.REQUIRED);
        }
        if (UnicodeText.length(trimmed) > MAX_LENGTH) {
            return Optional.of(InputProblem.TOO_LONG);
        }
        if (!FORMAT.matcher(trimmed).matches()) {
            return Optional.of(InputProblem.INVALID_FORMAT);
        }
        return Optional.empty();
    }

    /** ログインの失敗の記録に使う、ハッシュを返す。 */
    public String sha256() {
        return Sha256.hex(value);
    }

    private static String normalize(String input) {
        // 言語の設定に左右されない方法で、小文字にそろえる
        return UnicodeText.trim(input).toLowerCase(Locale.ROOT);
    }
}
