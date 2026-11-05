package bloodbank.domain;

/**
 * Пакета с таким id нет. Unchecked, как DuplicateBloodUnit.
 * Про код 404 класс ничего не знает: статус выбирает только handler (ApiErrors).
 */
public final class MissingBloodUnit extends RuntimeException {

    public MissingBloodUnit(String id) {
        super("Пакет не найден: " + id);
    }
}
