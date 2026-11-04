package bloodbank.handler;

import bloodbank.domain.DuplicateBloodUnit;
import bloodbank.domain.MissingBloodUnit;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.UUID;

/**
 * Единственное место, которое знает про коды 400, 404, 409, 415 и 500.
 * Исключения из domain про HTTP ничего не знают, здесь мы превращаем их в ApiError.
 */
@RestControllerAdvice
public class ApiErrors {

    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

    /** Пустое или слишком длинное поле: сервис даже не запускается. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        FieldError error = ex.getBindingResult().getFieldError();
        String message = error == null
                ? "request is not valid"
                : error.getField() + " " + error.getDefaultMessage();
        return answer(HttpStatus.BAD_REQUEST, "VALIDATION", message, correlation(req));
    }
    /** В пути не UUID (например номер BU-... вместо id). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> notUuid(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return answer(HttpStatus.BAD_REQUEST, "VALIDATION", ex.getName() + " must be a UUID", correlation(req));
    }

    /**
     * Тело запроса не читается. Чаще всего это не JSON, а если JSON верный, но в поле группы крови
     * или резуса стоит чужое слово, говорим какие слова можно.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> notJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        if (ex.getCause() instanceof InvalidFormatException wrong
                && wrong.getTargetType() != null && wrong.getTargetType().isEnum()
                && !wrong.getPath().isEmpty()) {
            String field = wrong.getPath().get(wrong.getPath().size() - 1).getFieldName();
            String allowed = Arrays.toString(wrong.getTargetType().getEnumConstants());
            return answer(HttpStatus.BAD_REQUEST, "VALIDATION", field + " must be one of " + allowed, correlation(req));
        }
        return answer(HttpStatus.BAD_REQUEST, "VALIDATION", "body must be JSON", correlation(req));
    }

    /** UUID правильный, но такой строки в таблице нет. */
    @ExceptionHandler(MissingBloodUnit.class)
    public ResponseEntity<ApiError> missing(MissingBloodUnit ex, HttpServletRequest req) {
        return answer(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), correlation(req));
    }

    /** Исключение из лабы 3: номер уже есть, первая строка осталась. */
    @ExceptionHandler(DuplicateBloodUnit.class)
    public ResponseEntity<ApiError> duplicate(DuplicateBloodUnit ex, HttpServletRequest req) {
        return answer(HttpStatus.CONFLICT, "DUPLICATE", ex.getMessage(), correlation(req));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> unsupportedType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return answer(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED", "content type is not supported", correlation(req));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> unsupportedMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return answer(HttpStatus.METHOD_NOT_ALLOWED, "UNSUPPORTED", "method is not supported", correlation(req));
    }

    /** Нет такого адреса вообще (не путать с 404 на несуществующий UUID). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> noResource(NoResourceFoundException ex, HttpServletRequest req) {
        return answer(HttpStatus.NOT_FOUND, "NOT_FOUND", "resource not found", correlation(req));
    }

    /** Всё остальное: ошибка у нас. Клиент видит только INTERNAL, а подробности остаются в логе. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        String id = correlation(req);
        log.error("unexpected error, correlationId={}", id, ex);
        return answer(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", "unexpected error", id);
    }

    private ResponseEntity<ApiError> answer(HttpStatus status, String code, String message, String correlationId) {
        return ResponseEntity.status(status).body(new ApiError(code, message, correlationId));
    }

    /** Если клиент прислал X-Correlation-Id, повторяем его. Если нет или пусто, придумываем свой. */
    private String correlation(HttpServletRequest req) {
        String id = req.getHeader("X-Correlation-Id");
        if (id == null || id.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return id;
    }
}
