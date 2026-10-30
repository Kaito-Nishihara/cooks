package app.recipebook.auth.session.domain;

/** セッションのトークンを、乱数から作る処理の約束。実装は基盤の層に置く。 */
public interface SessionTokenGenerator {

    /** 新しいトークンを作る。呼ぶたびに違う値になる。 */
    SessionToken generate();
}
