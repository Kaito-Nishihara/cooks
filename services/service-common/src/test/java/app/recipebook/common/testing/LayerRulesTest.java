package app.recipebook.common.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 層の決まりの道具が、破っているコードを見つけ、守っているコードを通すことを確かめる。
 * 見本は fixture の下にある。good は決まりを守り、bad は 1 つずつ破っている。
 */
class LayerRulesTest {

    private static final String GOOD = "app.recipebook.common.testing.fixture.good";
    private static final String BAD = "app.recipebook.common.testing.fixture.bad";

    private static JavaClasses classesOf(String basePackage) {
        return new ClassFileImporter().importPackages(basePackage);
    }

    private static String failuresOf(ArchRule rule) {
        return rule.evaluate(classesOf(BAD)).getFailureReport().toString();
    }

    @Test
    @DisplayName("決まりを守っている見本は、どの決まりにも通る")
    void goodFixturePassesAllRules() {
        JavaClasses classes = classesOf(GOOD);
        assertThat(classes).isNotEmpty();
        LayerRules rules = LayerRules.forService(GOOD);
        List.of(
                rules.domainUsesOnlyStandardPartsAndCore(),
                rules.applicationDoesNotUseOuterLayers(),
                rules.presentationDoesNotUseInfrastructureOrRepositories(),
                rules.domainHasNoFrameworkAnnotations())
                .forEach(rule -> rule.check(classes));
    }

    @Test
    @DisplayName("AC-SVC-18 ドメインが Jackson を使うと、見つける")
    void detectsDomainUsingFramework() {
        assertThat(failuresOf(LayerRules.forService(BAD).domainUsesOnlyStandardPartsAndCore()))
                .contains("bad.account.domain.Thing")
                .contains("ObjectMapper");
    }

    @Test
    @DisplayName("AC-SVC-19 ユースケースが基盤のクラスを使うと、見つける")
    void detectsApplicationUsingInfrastructure() {
        assertThat(failuresOf(LayerRules.forService(BAD).applicationDoesNotUseOuterLayers()))
                .contains("FindThingUseCase")
                .contains("MemoryThingRepository");
    }

    @Test
    @DisplayName("AC-SVC-19 入口がドメインのリポジトリを使うと、見つける")
    void detectsPresentationUsingRepository() {
        assertThat(failuresOf(LayerRules.forService(BAD).presentationDoesNotUseInfrastructureOrRepositories()))
                .contains("ThingEndpoint")
                .contains("ThingRepository");
    }

    @Test
    @DisplayName("AC-SVC-20 ドメインの項目に jakarta の注釈が付いていると、見つける")
    void detectsFrameworkAnnotationInDomain() {
        assertThat(failuresOf(LayerRules.forService(BAD).domainHasNoFrameworkAnnotations()))
                .contains("bad.account.domain.Thing")
                .contains("NotNull");
    }
}
