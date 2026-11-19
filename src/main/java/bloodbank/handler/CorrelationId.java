package bloodbank.handler;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Номер запроса для ответа с ошибкой. Им пользуются и ApiErrors (ошибки контроллера),
 * и SecurityErrors (401 и 403 из фильтров), чтобы правило было одно.
 */
public final class CorrelationId {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private CorrelationId() {
    }

    /**
     * Если клиент прислал нормальный X-Correlation-Id, повторяем его.
     * Нормальный это до 64 символов: буквы, цифры, точка, подчёркивание, дефис.
     * Если заголовка нет или он странный, придумываем свой, чтобы мусор не попал ни в лог, ни в ответ.
     */
    public static String from(HttpServletRequest req) {
        String id = req.getHeader("X-Correlation-Id");
        if (id == null || !SAFE_ID.matcher(id).matches()) {
            return UUID.randomUUID().toString();
        }
        return id;
    }
}
