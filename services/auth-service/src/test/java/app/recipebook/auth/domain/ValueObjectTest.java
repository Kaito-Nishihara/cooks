package app.recipebook.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.recipebook.auth.account.domain.DisplayName;
import app.recipebook.auth.account.domain.EmailAddress;
import app.recipebook.auth.account.domain.PasswordHash;
import app.recipebook.auth.account.domain.RawPassword;
import app.recipebook.auth.shared.domain.InputProblem;
import app.recipebook.auth.shared.domain.UnicodeText;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** 値オブジェクトの決まり(「機能設計: 登録とログイン」の 4.1 と 5.4)。Quarkus もデータベースも起動しない。 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValueObjectTest {

    @Test
    void 表示名は_NFC_で正規化し_前後の空白を取る() {
        // 「か」と濁点(U+3099)を、1 つの符号位置の「が」にまとめる
        assertThat(DisplayName.of(" 　がんも ").value()).isEqualTo("がんも");
        assertThat(DisplayName.of("山田 太郎").value()).isEqualTo("山田 太郎");
    }

    @Test
    void 表示名は_最初に当てはまる理由を_1_つ返す() {
        assertThat(DisplayName.check(null)).contains(InputProblem.REQUIRED);
        assertThat(DisplayName.check(" \t\n")).contains(InputProblem.REQUIRED);
        assertThat(DisplayName.check("a\tb")).contains(InputProblem.INVALID_FORMAT);
        assertThat(DisplayName.check("a‮b")).contains(InputProblem.INVALID_FORMAT);
        assertThat(DisplayName.check("a⁦b")).contains(InputProblem.INVALID_FORMAT);
        assertThat(DisplayName.check("a b")).contains(InputProblem.INVALID_FORMAT);
        assertThat(DisplayName.check("あ".repeat(51))).contains(InputProblem.TOO_LONG);
        // 改行があり、長すぎもするときは、表の上にある理由を返す
        assertThat(DisplayName.check("あ\n".repeat(40))).contains(InputProblem.INVALID_FORMAT);
        assertThat(DisplayName.check("😀".repeat(50))).isEmpty();
    }

    @Test
    void 表示名は_決まりに合わない値から作れない() {
        assertThatThrownBy(() -> DisplayName.of("　")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DisplayName(" みさき")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void メールアドレスは_前後の空白を取り_小文字にそろえる() {
        assertThat(EmailAddress.of("  Misaki@Example.COM ").value()).isEqualTo("misaki@example.com");
        // 言語の設定に左右されない。トルコ語の設定でも「I」は「i」になる
        assertThat(EmailAddress.of("INFO@EXAMPLE.COM").value()).isEqualTo("info@example.com");
        assertThat(EmailAddress.of("misaki+recipe@example.co.jp").value()).isEqualTo("misaki+recipe@example.co.jp");
    }

    @Test
    void メールアドレスは_形と長さを確かめる() {
        assertThat(EmailAddress.check("")).contains(InputProblem.REQUIRED);
        assertThat(EmailAddress.check("misaki@localhost")).contains(InputProblem.INVALID_FORMAT);
        assertThat(EmailAddress.check("misaki@example..com")).contains(InputProblem.INVALID_FORMAT);
        assertThat(EmailAddress.check("a".repeat(65) + "@example.com")).contains(InputProblem.INVALID_FORMAT);
        assertThat(EmailAddress.check("a".repeat(64) + "@example.com")).isEmpty();
        assertThat(EmailAddress.check("a@" + "b".repeat(250) + ".jp")).contains(InputProblem.TOO_LONG);
        assertThatThrownBy(() -> new EmailAddress("Misaki@example.com")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void メールアドレスのハッシュは_小文字にそろえた値から作る() {
        assertThat(EmailAddress.of("Misaki@Example.com").sha256())
                .isEqualTo(EmailAddress.of("misaki@example.com").sha256())
                .matches("[0-9a-f]{64}");
    }

    @Test
    void パスワードは_NFKC_で正規化し_前後の空白を取らない() {
        assertThat(RawPassword.forNew("Ｐａｓｓｗｏｒｄ２０２６！").reveal()).isEqualTo("Password2026!");
        assertThat(RawPassword.forNew(" abcdefghijkl ").reveal()).isEqualTo(" abcdefghijkl ");
    }

    @Test
    void パスワードの長さの下限は_新しく設定するときだけ確かめる() {
        assertThat(RawPassword.check("abcdefghi12", true)).contains(InputProblem.TOO_SHORT);
        assertThat(RawPassword.check("abcdefghi12", false)).isEmpty();
        assertThat(RawPassword.check("あ".repeat(12), true)).isEmpty();
        assertThat(RawPassword.check("", false)).contains(InputProblem.REQUIRED);
        assertThat(RawPassword.check("abc\u0000defghijkl", true)).contains(InputProblem.INVALID_FORMAT);
        assertThat(RawPassword.check("a".repeat(72), true)).isEmpty();
        assertThat(RawPassword.check("a".repeat(73), true)).contains(InputProblem.TOO_LONG);
        assertThat(RawPassword.check("あ".repeat(25), false)).contains(InputProblem.TOO_LONG);
    }

    @Test
    void パスワードは_文字列に直しても中身を出さない() {
        assertThat(RawPassword.forNew("ひみつのあいことば2026").toString()).doesNotContain("ひみつ");
        assertThat(new PasswordHash("$2a$12$" + "x".repeat(53)).toString()).doesNotContain("xxx");
    }

    @Test
    void 文字の数は_符号位置で数える() {
        assertThat(UnicodeText.length("𠮷")).isEqualTo(1);
        assertThat(UnicodeText.trim("　  a b  ")).isEqualTo("a b");
    }
}
