package app.recipebook.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * マイグレーション(「共通設計: データベースの共通仕様」の 5.9)。
 * 専用のスキーマを空にしてから起動するので、空のデータベースへの起動になる。
 */
@QuarkusTest
@TestProfile(MigrationTest.EmptySchema.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MigrationTest {

    /** 起動のたびに空にする、このテストだけのスキーマを使う。 */
    public static class EmptySchema implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.flyway.default-schema", "ac_db_01",
                    "quarkus.flyway.clean-at-start", "true",
                    "quarkus.flyway.clean-disabled", "false",
                    "quarkus.hibernate-orm.database.default-schema", "ac_db_01");
        }
    }

    @Inject
    Flyway flyway;

    @Inject
    DataSource dataSource;

    @Test
    @Order(1)
    @DisplayName("AC-DB-01 空のデータベースに対して起動すると、マイグレーションがすべて適用され、起動に成功する")
    void allMigrationsAreAppliedOnEmptyDatabase() throws Exception {
        // ここまで来ていれば、起動に成功している
        MigrationInfo[] all = flyway.info().all();
        assertThat(all).isNotEmpty();
        assertThat(all).allMatch(info -> info.getState() == MigrationState.SUCCESS);
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(columnsOfUsers()).contains("id", "email", "display_name", "password_hash", "created_at", "updated_at");
    }

    @Test
    @Order(2)
    @DisplayName("AC-DB-02 適用済みのデータベースに、起動のときと同じ適用をもう一度行うと、何も適用されず、成功する")
    void nothingIsAppliedOnMigratedDatabase() {
        int before = flyway.info().applied().length;

        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isZero();
        assertThat(flyway.info().applied()).hasSize(before);
    }

    private java.util.List<String> columnsOfUsers() throws Exception {
        java.util.List<String> columns = new java.util.ArrayList<>();
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "SELECT column_name FROM information_schema.columns"
                                + " WHERE table_schema = 'ac_db_01' AND table_name = 'users'");
                var rows = statement.executeQuery()) {
            while (rows.next()) {
                columns.add(rows.getString(1));
            }
        }
        return columns;
    }
}
