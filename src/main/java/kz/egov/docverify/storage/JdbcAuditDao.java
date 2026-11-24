package kz.egov.docverify.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.AuditEntry;

/** DAO: "audit_log" кестесі (әрекеттер журналы). */
public final class JdbcAuditDao implements Repository<AuditEntry, String> {
    private final Database db;

    public JdbcAuditDao(Database db) {
        this.db = db;
    }

    @Override
    public void save(AuditEntry e) {
        synchronized (db) {
            try (PreparedStatement ps = db.connection().prepareStatement(
                    "INSERT OR REPLACE INTO audit_log (id, doc_id, action, at) VALUES (?,?,?,?)")) {
                ps.setString(1, e.id());
                ps.setString(2, e.docId());
                ps.setString(3, e.action());
                ps.setString(4, e.at().toString());
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new DocVerifyException("Журналға жазу қатесі: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public Optional<AuditEntry> findById(String id) {
        synchronized (db) {
            try (PreparedStatement ps = db.connection().prepareStatement(
                    "SELECT id, doc_id, action, at FROM audit_log WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            } catch (SQLException ex) {
                throw new DocVerifyException("Журналдан оқу қатесі: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public List<AuditEntry> findAll() {
        synchronized (db) {
            try (PreparedStatement ps = db.connection().prepareStatement(
                    "SELECT id, doc_id, action, at FROM audit_log ORDER BY at");
                 ResultSet rs = ps.executeQuery()) {
                List<AuditEntry> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            } catch (SQLException ex) {
                throw new DocVerifyException("Журналдан оқу қатесі: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public boolean deleteById(String id) {
        synchronized (db) {
            try (PreparedStatement ps = db.connection().prepareStatement("DELETE FROM audit_log WHERE id = ?")) {
                ps.setString(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException ex) {
                throw new DocVerifyException("Журналдан жою қатесі: " + ex.getMessage(), ex);
            }
        }
    }

    private static AuditEntry map(ResultSet rs) throws SQLException {
        return new AuditEntry(rs.getString("id"), rs.getString("doc_id"), rs.getString("action"),
                Instant.parse(rs.getString("at")));
    }
}
