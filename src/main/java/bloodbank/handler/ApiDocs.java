package bloodbank.handler;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/** Название и описание API для Swagger (/swagger-ui.html, /v3/api-docs). */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Blood Bank API",
        version = "v1",
        description = "Учёт пакетов донорской крови: регистрация и чтение по id"))
public class ApiDocs {
}
