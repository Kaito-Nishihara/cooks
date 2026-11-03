package app.recipebook.common.openapi;

/**
 * 書き出す API の仕様の分け方(「共通設計: API の共通仕様」の 4.11、決定の記録 0064)。
 * 画面に出す /api と、サービスの間だけで使う /internal を、別の仕様にする。
 * 画面側の型に、使わない API が混ざらないようにするためである。
 */
public final class ApiDocuments {

    /** サービスの間の仕様の名前。設定 quarkus.smallrye-openapi.internal.* と、/q/openapi-internal で使う */
    public static final String INTERNAL = "internal";

    /** 画面に出す API のパスの始まり */
    static final String PUBLIC_PREFIX = "/api/";

    /** サービスの間だけで使う API のパスの始まり */
    static final String INTERNAL_PREFIX = "/internal/";

    private ApiDocuments() {
    }
}
