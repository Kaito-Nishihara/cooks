package app.recipebook.auth.shared.domain;

/**
 * 入力の値が決まりに合わないときの、合わない理由。
 * 値オブジェクトが判定し、入口の層が、応答の誤りの種類と文に置き換える。
 */
public enum InputProblem {
    /** ない。空白を取ると 0 文字 */
    REQUIRED,
    /** 使えない文字がある。形が合わない */
    INVALID_FORMAT,
    /** 短すぎる */
    TOO_SHORT,
    /** 長すぎる */
    TOO_LONG
}
