package app.recipebook.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** 起動に失敗するべき場合に、失敗することを確かめる。 */
class StartupFailureTest {

    /** 本物のマイグレーションの後に display_name の列を消したテーブルを、専用のスキーマに作る。 */
    public static class MissingColumn implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.flyway.locations", "db/migration,db/migration-missing-column",
                    "quarkus.flyway.default-schema", "ac_db_03",
                    "quarkus.flyway.clean-at-start", "true",
                    "quarkus.flyway.clean-disabled", "false",
                    "quarkus.hibernate-orm.database.default-schema", "ac_db_03");
        }
    }

    /** データベースのパスワードの設定を欠く。 */
    public static class MissingPassword implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.datasource.password", "");
        }
    }

    @Nested
    @QuarkusMainTest
    @TestProfile(MissingColumn.class)
    class WhenColumnIsMissing {

        @Test
        @Launch(value = {}, exitCode = 1)
        @DisplayName("AC-DB-03 Entity にあってテーブルにない列があると、起動に失敗する")
        void startupFails(LaunchResult result) {
            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.getOutput() + result.getErrorOutput()).contains("display_name");
        }
    }

    @Nested
    @QuarkusMainTest
    @TestProfile(MissingPassword.class)
    class WhenPasswordIsMissing {

        @Test
        @Launch(value = {}, exitCode = 1)
        @DisplayName("AC-SVC-16 データベースのパスワードの設定を欠いて起動すると、起動に失敗する")
        void startupFails(LaunchResult result) {
            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.getOutput() + result.getErrorOutput())
                    .contains("必要な設定がありません")
                    .contains("quarkus.datasource.password");
        }
    }
}
