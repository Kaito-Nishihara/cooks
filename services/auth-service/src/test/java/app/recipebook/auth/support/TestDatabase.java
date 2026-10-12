package app.recipebook.auth.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * テスト用の PostgreSQL を、管理者として操作する。
 * データベースが止まったときの動きを、サーバーを止めずに再現するために使う。
 */
public final class TestDatabase {

    private static final String ADMIN_URL = "jdbc:postgresql://postgres-test:5432/postgres";
    private static final String ADMIN_USER = "postgres";

    private TestDatabase() {
    }

    /** 新しい接続を拒み、今ある接続を切る。 */
    public static void refuseConnections(String database) {
        execute("ALTER DATABASE " + database + " ALLOW_CONNECTIONS false");
        execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + database + "'");
    }

    /** 接続を、また受け付ける。 */
    public static void acceptConnections(String database) {
        execute("ALTER DATABASE " + database + " ALLOW_CONNECTIONS true");
    }

    private static void execute(String sql) {
        // テスト用のデータベースは、パスワードを確かめない設定で動かしている
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, ADMIN_USER, "");
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("テスト用のデータベースを操作できない: " + sql, e);
        }
    }
}
