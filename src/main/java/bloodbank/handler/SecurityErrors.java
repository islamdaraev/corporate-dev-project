package bloodbank.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * 401 и 403 случаются в фильтрах, ещё до контроллера, поэтому @RestControllerAdvice их не видит
 * и тело было бы пустым. Здесь пишем тот же ApiError, что и везде: code, message, correlationId.
 */
public class SecurityErrors implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    public SecurityErrors(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** Нет токена, токен битый, подделан или просрочен: "я тебя не знаю". */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        write(request, response, 401, "UNAUTHENTICATED", "valid token required");
    }

    /** Токен хороший, но роль не подходит: "я тебя знаю, но нет". */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        write(request, response, 403, "FORBIDDEN", "not allowed for your role");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), new ApiError(code, message, CorrelationId.from(request)));
    }
}
