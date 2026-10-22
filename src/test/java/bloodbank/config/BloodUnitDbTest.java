package bloodbank.config;

import bloodbank.domain.BloodUnitId;
import bloodbank.domain.BloodUnitNumber;
import bloodbank.domain.BloodUnitRepository;
import bloodbank.domain.DuplicateBloodUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// НЕТ @Transactional на классе и на методах:
// тест должен видеть то, что реально сохранилось
@SpringBootTest(classes = Application.class)
class BloodUnitDbTest {

    @Autowired
    BloodUnitService service;

    @Autowired
    BloodUnitRepository units;

    @Autowired
    JdbcTemplate jdbc;

    // Таблицу создал Flyway (V1) при старте. Мы только чистим строки, таблицу не трогаем:
    // DROP TABLE оставил бы в flyway_schema_history запись, что таблица есть.
    @BeforeEach
    void emptyTable() {
        jdbc.update("DELETE FROM blood_unit");
    }

    @Test
    void secondStatementRollsBack() {
        BloodUnitNumber number = new BloodUnitNumber("BU-2026-000001");

        assertThrows(DuplicateBloodUnit.class, () -> service.insertTwice(number));

        assertEquals(0, units.count("BU-2026-000001"));
    }

    @Test
    void secondRequestKeepsTheFirst() {
        BloodUnitNumber number = new BloodUnitNumber("BU-2026-000002");
        service.register(BloodUnitId.newId(), number, "Первая заявка");

        assertThrows(DuplicateBloodUnit.class,
                () -> service.register(BloodUnitId.newId(), number, "Ещё раз"));

        assertEquals(1, units.count("BU-2026-000002"));
    }
}
