package bloodbank.handler;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Название и описание API для Swagger (/swagger-ui.html, /v3/api-docs).
 * Схема bearerAuth даёт в Swagger кнопку Authorize: вставляешь токен из /api/v1/auth/login,
 * и Swagger сам добавляет заголовок Authorization: Bearer к запросам.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Blood Bank API",
        version = "v1",
        description = "Учёт пакетов донорской крови: вход, регистрация, чтение и смена статуса"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class ApiDocs {
}
