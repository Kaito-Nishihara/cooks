package app.recipebook.book.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** テスト用の PostgreSQL を、管理者として操作する。 */
public final class TestDatabase {

    /** テスト用のサーバーの場所。データベースの名前を後ろに付けて使う */
    public static final String SERVER = "jdbc:postgresql://postgres-test:5432/";
    private static final String ADMIN_USER = "postgres";

    private TestDatabase() {
    }

    /** 新しい接続を拒み、今ある接続を切る。データベースが止まったときの動きを、サーバーを止めずに再現する。 */
    public static void refuseConnections(String database) {
        execute("ALTER DATABASE " + database + " ALLOW_CONNECTIONS false");
        execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + database + "'");
    }

    /** 接続を、また受け付ける。 */
    public static void acceptConnections(String database) {
        execute("ALTER DATABASE " + database + " ALLOW_CONNECTIONS true");
    }

    /** 指定の利用者で、指定のデータベースに接続する。テスト用のサーバーは、パスワードを確かめない。 */
    public static Connection connectAs(String user, String database) throws SQLException {
        return DriverManager.getConnection(SERVER + database, user, "");
    }

    private static void execute(String sql) {
        try (Connection connection = connectAs(ADMIN_USER, "postgres"); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("テスト用のデータベースを操作できない: " + sql, e);
        }
    }
}
