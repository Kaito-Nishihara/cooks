package app.recipebook.common.testing;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Objects;

/**
 * 層の決まり(「共通設計: サービスの共通構成」の 4.2)を確かめるための、テストの道具。
 * 3 つのサービスが、同じ決まりを同じ書き方で確かめられるように、ここに 1 つだけ置く。
 * テストだけで使う。動かすときのコードから呼ばない。
 */
public final class LayerRules {

    /** ドメインが使ってよい、共通のライブラリの部分 */
    private static final String COMMON_CORE = "app.recipebook.common.core..";
    /** ドメインに持ち込まない、枠組みのパッケージ */
    private static final String[] FRAMEWORKS = {"jakarta..", "io.quarkus..", "com.fasterxml.."};

    private final String domain;
    private final String application;
    private final String presentation;
    private final String infrastructure;
    private final String basePackage;

    private LayerRules(String basePackage) {
        this.basePackage = Objects.requireNonNull(basePackage, "basePackage");
        this.domain = basePackage + "..domain..";
        this.application = basePackage + "..application..";
        this.presentation = basePackage + "..presentation..";
        this.infrastructure = basePackage + "..infrastructure..";
    }

    /** サービスのいちばん上のパッケージを渡す。例: app.recipebook.auth */
    public static LayerRules forService(String basePackage) {
        return new LayerRules(basePackage);
    }

    /** このサービスの、動かすときのクラスを読み込む。テストのクラスは含めない。 */
    public JavaClasses importClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(basePackage);
    }

    /** AC-SVC-18: ドメインは、Java の標準の部品と、共通の core と、自分のドメインだけを使う。 */
    public ArchRule domainUsesOnlyStandardPartsAndCore() {
        return classes().that().resideInAPackage(domain)
                .should().onlyDependOnClassesThat(resideInAnyPackage("java..", COMMON_CORE, domain))
                .allowEmptyShould(true)
                .as("AC-SVC-18 ドメインは、Java の標準の部品、common.core、自分のドメインのほかを使わない");
    }

    /** AC-SVC-19 の前半: ユースケースは、基盤と入口を使わない。 */
    public ArchRule applicationDoesNotUseOuterLayers() {
        return noClasses().that().resideInAPackage(application)
                .should().dependOnClassesThat(resideInAnyPackage(infrastructure, presentation))
                .allowEmptyShould(true)
                .as("AC-SVC-19 ユースケースは、基盤と入口のクラスを使わない");
    }

    /** AC-SVC-19 の後半: 入口は、基盤と、ドメインのリポジトリを使わない。 */
    public ArchRule presentationDoesNotUseInfrastructureOrRepositories() {
        DescribedPredicate<JavaClass> repository = describe("ドメインのリポジトリ",
                type -> resideInAPackage(domain).test(type) && simpleNameEndingWith("Repository").test(type));
        return noClasses().that().resideInAPackage(presentation)
                .should().dependOnClassesThat(resideInAPackage(infrastructure).or(repository))
                .allowEmptyShould(true)
                .as("AC-SVC-19 入口は、基盤のクラスと、ドメインのリポジトリを使わない");
    }

    /** AC-SVC-20: ドメインのクラスと、その項目やメソッドに、枠組みの注釈を付けない。 */
    public ArchRule domainHasNoFrameworkAnnotations() {
        return noClasses().that().resideInAPackage(domain)
                .should().dependOnClassesThat(resideInAnyPackage(FRAMEWORKS))
                .allowEmptyShould(true)
                .as("AC-SVC-20 ドメインに、jakarta、io.quarkus、com.fasterxml の注釈を付けない");
    }
}
