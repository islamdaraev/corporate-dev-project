package bloodbank;

/**
 * Правила перевода пакета крови из одного статуса в другой.
 * Разрешённый переход возвращает новый статус.
 * Запрещённый переход бросает IllegalStateException.
 */
public final class BloodUnitPolicy {

    public BloodUnitStatus move(BloodUnitStatus from, BloodUnitStatus to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Статус не может быть null");
        }

        boolean allowed = switch (from) {
            case COLLECTED -> to == BloodUnitStatus.TESTED   || to == BloodUnitStatus.DISCARDED;
            case TESTED    -> to == BloodUnitStatus.RELEASED || to == BloodUnitStatus.DISCARDED;
            case RELEASED  -> to == BloodUnitStatus.ISSUED   || to == BloodUnitStatus.DISCARDED;
            case ISSUED    -> false; // выданный пакет — конечный статус
            case DISCARDED -> false; // утилизированный пакет — конечный статус
        };

        if (allowed) {
            return to;
        }
        throw new IllegalStateException("Нельзя перевести пакет из " + from + " в " + to);
    }
}
