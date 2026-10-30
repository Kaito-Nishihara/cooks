package app.recipebook.auth.support;

import io.quarkus.test.common.QuarkusTestResource;

/**
 * テスト用の署名の鍵を、このサービスのすべてのテストに使わせるための印。
 * restrictToAnnotatedClass を false にしているので、このクラスに付けた資源が、すべてのテストに効く。
 */
@QuarkusTestResource(value = TestSigningKeys.class, restrictToAnnotatedClass = false)
public final class UsesTestSigningKeys {

    private UsesTestSigningKeys() {
    }
}
