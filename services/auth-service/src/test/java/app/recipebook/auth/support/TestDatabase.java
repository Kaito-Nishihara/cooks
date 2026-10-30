package app.recipebook.auth.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * テスト用の PostgreSQL を、管理者として操作する。
 * テストの前にテーブルを空にすることと、保存された中身を確かめることに使う。
 */
public final class TestDatabase {

    private static final String SERVER = "jdbc:postgresql://postgres-test:5432/";
    private static final String ADMIN_USER = "postgres";
    private static final String DATABASE = "auth_test";
    private static final List<String> TABLES = List.of("users", "sessions", "login_attempts");

    private TestDatabase() {
    }

    /** このサービスのテーブルを、すべて空にする。 */
    public static void truncateAll() {
        execute(DATABASE, "TRUNCATE " + String.join(", ", TABLES) + " CASCADE");
    }

    /** テーブルの行の数を返す。 */
    public static int count(String table) {
        return Integer.parseInt(queryStrings("SELECT count(*) FROM " + table).get(0));
    }

    /** すべてのテーブルの、すべての列の値を、1 つの文字列にして返す。値が入っていないことを確かめるのに使う。 */
    public static String dumpAll() {
        StringBuilder dump = new StringBuilder();
        for (String table : TABLES) {
            queryStrings("SELECT row_to_json(t)::text FROM " + table + " t").forEach(row -> dump.append(row).append('\n'));
        }
        return dump.toString();
    }

    /** 1 列の結果を、文字列の一覧で返す。 */
    public static List<String> queryStrings(String sql) {
        List<String> values = new ArrayList<>();
        try (Connection connection = connect(DATABASE);
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                values.add(rows.getString(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("テスト用のデータベースを読めない: " + sql, e);
        }
        return values;
    }

    /** 新しい接続を拒み、今ある接続を切る。データベースが止まったときの動きを、サーバーを止めずに再現する。 */
    public static void refuseConnections(String database) {
        execute("postgres", "ALTER DATABASE " + database + " ALLOW_CONNECTIONS false");
        execute("postgres", "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + database + "'");
    }

    /** 接続を、また受け付ける。 */
    public static void acceptConnections(String database) {
        execute("postgres", "ALTER DATABASE " + database + " ALLOW_CONNECTIONS true");
    }

    private static void execute(String database, String sql) {
        try (Connection connection = connect(database); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("テスト用のデータベースを操作できない: " + sql, e);
        }
    }

    private static Connection connect(String database) throws SQLException {
        // テスト用のデータベースは、パスワードを確かめない設定で動かしている
        return DriverManager.getConnection(SERVER + database, ADMIN_USER, "");
    }
}
