package app.recipebook.book;

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

/** 必要な設定が欠けているときに、起動に失敗することを確かめる(「共通設計: サービスの共通構成」の 4.7)。 */
class StartupFailureTest {

    /** データベースのパスワードの設定を欠く。 */
    public static class MissingPassword implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.datasource.password", "");
        }
    }

    /** auth-service の公開の鍵の場所を欠く。 */
    public static class MissingKeyLocation implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("mp.jwt.verify.publickey.location", "");
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
            assertThat(result.getOutput() + result.getErrorOutput())
                    .contains("必要な設定がありません")
                    .contains("quarkus.datasource.password");
        }
    }

    @Nested
    @QuarkusMainTest
    @TestProfile(MissingKeyLocation.class)
    class WhenKeyLocationIsMissing {

        @Test
        @Launch(value = {}, exitCode = 1)
        @DisplayName("公開の鍵の場所の設定を欠いて起動すると、起動に失敗し、欠けた設定の名前が出る")
        void startupFails(LaunchResult result) {
            // smallrye-jwt が、RequiredSettings より先にこの設定を読んで失敗する。どちらの失敗でも、設定の名前は出る
            assertThat(result.getOutput() + result.getErrorOutput())
                    .contains("mp.jwt.verify.publickey.location");
        }
    }
}
