package bloodbank.persistence;

import bloodbank.domain.BloodUnitRepository;
import bloodbank.domain.DuplicateBloodUnit;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Только SQL: записать пакет и посчитать пакеты по номеру. */
@Repository
public class BloodUnitJdbc implements BloodUnitRepository {

    private final JdbcTemplate jdbc;

    public BloodUnitJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(UUID id, String businessKey, String status, String title) {
        try {
            jdbc.update("""
                    INSERT INTO blood_unit (id, business_key, status, title)
                    VALUES (?, ?, ?, ?)
                    """, id, businessKey, status, title);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateBloodUnit(businessKey); // язык базы переводим на язык domain
        }
    }

    @Override
    public int count(String businessKey) {
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM blood_unit WHERE business_key = ?",
                Integer.class,
                businessKey);
        return n == null ? 0 : n;
    }
}

