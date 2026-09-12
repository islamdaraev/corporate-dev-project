package bloodbank;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BloodUnitPolicyTest {

    private final BloodUnitPolicy policy = new BloodUnitPolicy();

    /**
     * Четыре строки из README: два разрешённых перехода и два запрещённых.
     * allowed = true  -> move возвращает новый статус
     * allowed = false -> move бросает IllegalStateException
     * следующий метод запустится несколько раз — по разу на каждую строку данных из @CsvSource
     * Начало данных для теста.
     * • @CsvSource — аннотация «вот данные»
     * • ({ — открывает список строк
     */
    @ParameterizedTest
    @CsvSource({
            "COLLECTED, TESTED,   true",
            "TESTED,    RELEASED, true",
            "COLLECTED, ISSUED,   false",
            "ISSUED,    RELEASED, false"
    })
    void readmeRows(BloodUnitStatus from, BloodUnitStatus to, boolean allowed) {
        if (allowed) {
            assertEquals(to, policy.move(from, to));
        } else {
            assertThrows(IllegalStateException.class, () -> policy.move(from, to));
        }
    }

    @Test
    void nullIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BloodUnitId(null));
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BloodUnitId("   "));
    }
}
