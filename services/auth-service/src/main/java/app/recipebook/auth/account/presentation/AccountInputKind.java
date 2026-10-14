package app.recipebook.auth.account.presentation;

import app.recipebook.auth.account.domain.DisplayName;
import app.recipebook.auth.account.domain.EmailAddress;
import app.recipebook.auth.account.domain.RawPassword;
import app.recipebook.auth.shared.domain.InputProblem;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * 入力の値の種類と、誤りを伝える文(「機能設計: 登録とログイン」の 4.1)。
 * 決まりそのものは値オブジェクトが持ち、ここでは、種類ごとの文だけを決める。
 */
public enum AccountInputKind {

    DISPLAY_NAME(DisplayName::check, Map.of(
            InputProblem.REQUIRED, "表示名を入れてください。",
            InputProblem.INVALID_FORMAT, "使えない文字が入っています。改行などを取り除いてください。",
            InputProblem.TOO_LONG, "50 文字までで入れてください。")),

    EMAIL(EmailAddress::check, Map.of(
            InputProblem.REQUIRED, "メールアドレスを入れてください。",
            InputProblem.TOO_LONG, "254 文字までで入れてください。",
            InputProblem.INVALID_FORMAT, "メールアドレスの形が正しくありません。半角の英数字と記号で入れてください。")),

    /** 今のパスワードと比べる値。長さの下限を適用しない */
    PASSWORD(input -> RawPassword.check(input, false), passwordMessages()),

    /** 新しく設定するパスワード。長さの下限を適用する */
    NEW_PASSWORD(input -> RawPassword.check(input, true), passwordMessages());

    private final Function<String, Optional<InputProblem>> check;
    private final Map<InputProblem, String> messages;

    AccountInputKind(Function<String, Optional<InputProblem>> check, Map<InputProblem, String> messages) {
        this.check = check;
        this.messages = messages;
    }

    Optional<InputProblem> check(String input) {
        return check.apply(input);
    }

    String messageOf(InputProblem problem) {
        return messages.get(problem);
    }

    private static Map<InputProblem, String> passwordMessages() {
        return Map.of(
                InputProblem.REQUIRED, "パスワードを入れてください。",
                InputProblem.INVALID_FORMAT, "使えない文字が入っています。",
                InputProblem.TOO_SHORT, "12 文字以上で入れてください。",
                InputProblem.TOO_LONG, "パスワードが長すぎます。半角なら 72 文字、全角なら 24 文字までを目安に短くしてください。");
    }
}
