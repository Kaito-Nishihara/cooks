package app.recipebook.auth.account.domain;

/**
 * パスワードをハッシュにする処理の約束。
 * ドメインを暗号のライブラリに頼らせないために、実装は基盤の層に置く。
 */
public interface PasswordHasher {

    /** ハッシュにする。同じパスワードでも、呼ぶたびに違う値になる。 */
    PasswordHash hash(RawPassword password);

    /** パスワードが、ハッシュのもとになった値と同じかどうかを返す。 */
    boolean matches(RawPassword password, PasswordHash hash);

    /**
     * 比べる相手がいないときに、同じだけの計算を行う。
     * 登録のないメールアドレスのときだけ応答が速くなると、登録の有無を外から調べられるためである。
     */
    void burn(RawPassword password);
}
