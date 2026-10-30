package app.recipebook.auth.token.application;

import app.recipebook.auth.shared.domain.UserId;
import java.time.Instant;

/**
 * ほかのサービスへ渡すトークンを作る処理の約束(「共通設計: 認証」の 4.7)。
 * 実装は基盤の層に置く。
 */
public interface AccessTokenIssuer {

    /** トークンの有効な時間。1 つの要求のために作り、その要求が終われば捨てる */
    long LIFETIME_SECONDS = 60;

    /** この利用者のトークンを作る。メールアドレスと表示名と役割は入れない。 */
    String issue(UserId userId, Instant now);
}
