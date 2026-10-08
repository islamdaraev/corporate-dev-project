package bloodbank.domain;

/**
 * Пакет с таким номером уже есть. Unchecked, чтобы транзакция откатывалась.
 * ACID: A — Atomicity. Из-за RuntimeException Spring делает rollback,
 * и первая вставка из той же транзакции тоже пропадает.
 */
public final class DuplicateBloodUnit extends RuntimeException {

    public DuplicateBloodUnit(String businessKey) {
        super("Пакет с таким номером уже есть: " + businessKey);
    }
}
