package bloodbank.domain;
/** только таблица переходов.*/
public final class TransitionRule implements Rule {

    @Override
    public void check(BloodUnitStatus from, BloodUnitStatus to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Статус не может быть null");
        }

        boolean allowed = switch (from) {
            case COLLECTED -> to == BloodUnitStatus.TESTED   || to == BloodUnitStatus.DISCARDED;
            case TESTED    -> to == BloodUnitStatus.RELEASED || to == BloodUnitStatus.DISCARDED;
            case RELEASED  -> to == BloodUnitStatus.ISSUED   || to == BloodUnitStatus.DISCARDED;
            case ISSUED    -> false;
            case DISCARDED -> false;
        };

        if (!allowed) {
            throw new IllegalStateException("Нельзя перевести пакет из " + from + " в " + to);
        }
    }
}
