package bloodbank.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodUnitPolicyTest {

    private final BloodUnitPolicy policy = new BloodUnitPolicy();

    /**
     * Четыре строки из README: два разрешённых перехода и два запрещённых.
     * allowed = true  -> move возвращает новый статус
     * allowed = false -> move бросает IllegalStateException
     *
     * @CsvSource аннотация «вот данные»
     * открывает список строк
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
    void forbiddenMoveExplainsItself() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> policy.move(BloodUnitStatus.COLLECTED, BloodUnitStatus.ISSUED));

        assertTrue(error.getMessage().contains("COLLECTED"));
        assertTrue(error.getMessage().contains("ISSUED"));
    }

    @Test
    void nullIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BloodUnitId(null));
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BloodUnitId("   "));
    }

    @Test
    void notUuidIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BloodUnitId("BU-2026-000123"));
    }

    @Test
    void nullNumberIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BloodUnitNumber(null));
    }

    @Test
    void blankNumberIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BloodUnitNumber("   "));
    }
}
