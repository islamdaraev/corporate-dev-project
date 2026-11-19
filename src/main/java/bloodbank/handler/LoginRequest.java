package bloodbank.handler;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Что присылает клиент на вход. Пароль виден только в этом одном запросе:
 * в ответ, в токен и в лог он не попадает. Проверки живут здесь, в handler.
 */
public record LoginRequest(
        @Schema(description = "Имя пользователя", example = "technician")
        @NotBlank(message = "must not be blank") String username,
        @Schema(description = "Пароль", example = "tech-dev")
        @NotBlank(message = "must not be blank") String password) {
}
