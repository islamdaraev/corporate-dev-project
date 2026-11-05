package bloodbank.config;

import bloodbank.domain.BloodGroup;
import bloodbank.domain.BloodUnitId;
import bloodbank.domain.BloodUnitNumber;
import bloodbank.domain.BloodUnitView;
import bloodbank.domain.RhFactor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Как и BloodUnitDbTest: настоящая база, без @Transactional, таблицу создаёт Flyway (V1 и V2)
@SpringBootTest(classes = Application.class)
class BloodUnitGroupDbTest {

    @Autowired
    BloodUnitService service;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void emptyTable() {
        jdbc.update("DELETE FROM blood_unit");
    }

    @Test
    void groupAndRhAreSavedAndReadBack() {
        BloodUnitId id = BloodUnitId.newId();
        service.register(id, new BloodUnitNumber("BU-2026-000010"), "Цельная кровь", BloodGroup.A, RhFactor.POSITIVE);

        BloodUnitView found = service.require(id);

        assertEquals(BloodGroup.A, found.bloodGroup());
        assertEquals(RhFactor.POSITIVE, found.rhFactor());
    }

    @Test
    void unitWithoutGroupIsSavedWithNulls() {
        BloodUnitId id = BloodUnitId.newId();
        service.register(id, new BloodUnitNumber("BU-2026-000011"), "Без группы");

        BloodUnitView found = service.require(id);

        assertNull(found.bloodGroup());
        assertNull(found.rhFactor());
    }

    @Test
    void databaseRefusesAWordThatIsNotInTheCheck() {
        // даже если Java забудет проверить, CHECK из V2 не пустит чужое слово
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO blood_unit (id, business_key, status, title, blood_group) "
                        + "VALUES (gen_random_uuid(), 'BU-2026-000012', 'COLLECTED', 'x', 'Z')"));
    }
}
