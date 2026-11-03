package app.recipebook.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.recipebook.book.support.TestDatabase;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * サービスごとのデータベースの分け方(「共通設計: データベースの共通仕様」の 5.1)。
 * 接続する利用者が、自分のデータベースにだけ接続できることを確かめる。Quarkus を起動しない。
 */
class DatabaseIsolationTest {

    private static final List<String> SERVICES = List.of("auth", "book", "kitchen");

    /** 利用者と、接続しようとするデータベースの、すべての組み合わせ。 */
    static Stream<Arguments> userAndDatabase() {
        return SERVICES.stream().flatMap(user -> SERVICES.stream().map(database -> Arguments.of(user, database)));
    }

    @ParameterizedTest(name = "{0}_app で {1}_test に接続する")
    @MethodSource("userAndDatabase")
    @DisplayName("AC-DB-04 接続する利用者は、自分のデータベースに接続でき、ほかのサービスのデータベースへの接続は拒まれる")
    void userConnectsOnlyToOwnDatabase(String user, String database) throws SQLException {
        String role = user + "_app";
        String target = database + "_test";
        if (user.equals(database)) {
            try (Connection connection = TestDatabase.connectAs(role, target)) {
                assertThat(connection.isValid(3)).isTrue();
            }
        } else {
            // 42501 は、権限がないことを表す PostgreSQL の誤りの番号である
            assertThatThrownBy(() -> TestDatabase.connectAs(role, target).close())
                    .isInstanceOf(SQLException.class)
                    .satisfies(e -> assertThat(((SQLException) e).getSQLState()).isEqualTo("42501"));
        }
    }
}
