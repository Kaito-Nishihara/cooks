package app.recipebook.auth;

import app.recipebook.common.testing.LayerRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 層の決まりを確かめる(「共通設計: サービスの共通構成」の 4.2)。Quarkus を起動しない。 */
class LayerRulesTest {

    private static final LayerRules RULES = LayerRules.forService("app.recipebook.auth");
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = RULES.importClasses();
    }

    @Test
    @DisplayName("AC-SVC-18 domain のクラスが、Java の標準の部品、common.core、自分の domain のほかを使っていない")
    void domainUsesOnlyStandardPartsAndCore() {
        RULES.domainUsesOnlyStandardPartsAndCore().check(classes);
    }

    @Test
    @DisplayName("AC-SVC-19 application のクラスが、infrastructure と presentation のクラスを使っていない")
    void applicationDoesNotUseOuterLayers() {
        RULES.applicationDoesNotUseOuterLayers().check(classes);
    }

    @Test
    @DisplayName("AC-SVC-19 presentation のクラスが、infrastructure と domain のリポジトリを使っていない")
    void presentationDoesNotUseInfrastructureOrRepositories() {
        RULES.presentationDoesNotUseInfrastructureOrRepositories().check(classes);
    }

    @Test
    @DisplayName("AC-SVC-20 domain のクラスに、jakarta、io.quarkus、com.fasterxml の注釈が付いていない")
    void domainHasNoFrameworkAnnotations() {
        RULES.domainHasNoFrameworkAnnotations().check(classes);
    }
}
