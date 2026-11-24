package kz.egov.docverify.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import kz.egov.docverify.exception.DocVerifyException;
import kz.egov.docverify.model.DocumentType;
import kz.egov.docverify.model.RegistryEntry;
import kz.egov.docverify.model.RegistryStatus;

/** DAO: "registry" кестесімен JDBC арқылы жұмыс (тек PreparedStatement, SQL-injection жоқ). */
public final class JdbcRegistryDao implements Repository<RegistryEntry, String> {
    private final Database db;

    public JdbcRegistryDao(Database db) {
        this.db = db;
    }

    @Override
    public void save(RegistryEntry e) {
        synchronized (db) {
            try (PreparedStatement ps = db.connection().prepareStatement(
                    "INSERT OR REPLACE INTO registry (id, type, issued_on, valid_until, status) VALUES (?,?,?,?,?)")) {
                ps.setString(1, e.id());
                ps.setString(2, e.type().name());
                ps.setString(3, e.issuedOn().toString());
                ps.setString(4, e.validUntil().toString());
                ps.setString(5, e.status().name());
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new DocVerifyException("Тізілімге жазу қатесі: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public Optional<RegistryEntry> findById(String id) {
        synchronized (db) {
            try (PreparedStatement ps = db.connection().prepareStatement(
                    "SELECT id, type, issued_on, valid_until, status FROM registry WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            } catch (SQLException ex) {
                throw new DocVerifyException("Тізілімнен оқу қатесі: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public List<RegistryEntry> findAll() {
        synchronized (db) {
            try (PreparedStatement ps = db.connection().prepareStatement(
                    "SELECT id, type, issued_on, valid_until, status FROM registry ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                List<RegistryEntry> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            } catch (SQLException ex) {
                throw new DocVerifyException("Тізілімнен оқу қатесі: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public boolean deleteById(String id) {
        synchronized (db) {
            try (PreparedStatement ps = db.connection().prepareStatement("DELETE FROM registry WHERE id = ?")) {
                ps.setString(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException ex) {
                throw new DocVerifyException("Тізілімнен жою қатесі: " + ex.getMessage(), ex);
            }
        }
    }

    private static RegistryEntry map(ResultSet rs) throws SQLException {
        return new RegistryEntry(
                rs.getString("id"),
                DocumentType.valueOf(rs.getString("type")),
                LocalDate.parse(rs.getString("issued_on")),
                LocalDate.parse(rs.getString("valid_until")),
                RegistryStatus.valueOf(rs.getString("status")));
    }
}
