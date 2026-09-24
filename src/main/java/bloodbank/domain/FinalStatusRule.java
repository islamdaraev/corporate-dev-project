package bloodbank.domain;
/** только проверка конечных статусов.*/
public final class FinalStatusRule implements Rule {

    @Override
    public void check(BloodUnitStatus from, BloodUnitStatus to) {
        if (from == BloodUnitStatus.ISSUED || from == BloodUnitStatus.DISCARDED) {
            throw new IllegalStateException(
                "Пакет в конечном статусе " + from + ", движение невозможно"
            );
        }
    }
}
