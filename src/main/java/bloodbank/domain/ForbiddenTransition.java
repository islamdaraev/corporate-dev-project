package bloodbank.domain;

/**
 * Правила не разрешают такой переход статуса (например, выдать пакет, который ещё не допущен к выдаче).
 * Unchecked, как DuplicateBloodUnit. Про код 409 класс ничего не знает: его выбирает handler (ApiErrors).
 */
public final class ForbiddenTransition extends RuntimeException {

    public ForbiddenTransition(String message) {
        super(message);
    }
}
