package app.recipebook.auth.token.application;

import java.util.List;

/** ほかのサービスに配る、公開の鍵の一覧の約束。実装は基盤の層に置く。 */
public interface PublicKeySet {

    /** 今の署名の鍵と、入れ替えの間だけ残す古い鍵の、公開の部分を返す。 */
    List<PublicKeyView> publish();
}
