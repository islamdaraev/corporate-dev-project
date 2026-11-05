package bloodbank.handler;

/**
 * Тело любой ошибки: код, сообщение и номер запроса. Больше ничего:
 * ни стека, ни имени класса исключения.
 */
public record ApiError(String code, String message, String correlationId) {
}
