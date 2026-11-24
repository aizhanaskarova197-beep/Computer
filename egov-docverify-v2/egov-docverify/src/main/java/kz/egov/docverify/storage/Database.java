package kz.egov.docverify.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;

import kz.egov.docverify.exception.DocVerifyException;

/**
 * SQLite байланысы (JDBC). Бір байланыс, барлық қолжетім synchronized.
 * execute(...) ішіндегі барлық DAO әрекеттері бір транзакцияға кіреді:
 * қате болса, rollback жасалады.
 */
public final class Database implements TransactionManager, AutoCloseable {
    private final Connection conn;
    private int depth = 0;

    /** url мысалы: "jdbc:sqlite:data/registry.db" немесе "jdbc:sqlite::memory:". */
    public Database(String url) {
        try {
            conn = DriverManager.getConnection(url);
            createSchema();
        } catch (SQLException e) {
            throw new DocVerifyException("Дерекқорға қосылу мүмкін болмады: " + e.getMessage(), e);
        }
    }

    private void createSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS registry ("
                    + "id TEXT PRIMARY KEY, type TEXT NOT NULL, issued_on TEXT NOT NULL, "
                    + "valid_until TEXT NOT NULL, status TEXT NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS audit_log ("
                    + "id TEXT PRIMARY KEY, doc_id TEXT NOT NULL, action TEXT NOT NULL, at TEXT NOT NULL)");
        }
    }

    public Connection connection() {
        return conn;
    }

    @Override
    public synchronized <R> R execute(Callable<R> work) {
        boolean outer = depth == 0;
        depth++;
        try {
            if (outer) {
                conn.setAutoCommit(false);
            }
            R result = work.call();
            if (outer) {
                conn.commit();
            }
            return result;
        } catch (Exception e) {
            if (outer) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                    // rollback сәтсіз болса, бастапқы қатені жоғалтпаймыз
                }
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new DocVerifyException("Транзакция қатесі: " + e.getMessage(), e);
        } finally {
            depth--;
            if (outer) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // жабық байланыс
                }
            }
        }
    }

    @Override
    public synchronized void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new DocVerifyException("Дерекқорды жабу қатесі: " + e.getMessage(), e);
        }
    }
}
