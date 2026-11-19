package bloodbank.handler;

import bloodbank.config.BloodUnitService;
import bloodbank.domain.BloodUnitId;
import bloodbank.domain.BloodUnitNumber;
import bloodbank.domain.BloodUnitView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Один ресурс: пакет крови. POST создаёт, GET читает, три глагола меняют статус
 * (test, release и закрытый issue). Контроллер выбирает код ответа, а работу делает сервис.
 * Кто звонит, берём из Authentication (имя из токена), а не из JSON: в теле имя можно написать любое.
 * В этом классе нет EntityManager и нет сущности.
 */
@RestController
@RequestMapping("/api/v1/blood-units")
@SecurityRequirement(name = "bearerAuth")
public class BloodUnitController {

    private final BloodUnitService service;

    public BloodUnitController(BloodUnitService service) {
        this.service = service;
    }

    @Operation(summary = "Зарегистрировать пакет крови")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Создан. Заголовок Location это адрес GET"),
            @ApiResponse(responseCode = "400", description = "Номер или название пустое, название длиннее 120, неверная группа крови или резус"),
            @ApiResponse(responseCode = "401", description = "Токена нет, он битый или просрочен"),
            @ApiResponse(responseCode = "409", description = "Пакет с таким номером уже есть")
    })
    @PostMapping
    public ResponseEntity<BloodUnitResponse> create(@Valid @RequestBody CreateBloodUnit body) {
        BloodUnitView created = service.register(
                BloodUnitId.newId(), new BloodUnitNumber(body.businessKey()), body.title(),
                body.bloodGroup(), body.rhFactor());
        URI location = URI.create("/api/v1/blood-units/" + created.id());
        return ResponseEntity.created(location).body(BloodUnitResponse.from(created));
    }

    @Operation(summary = "Прочитать пакет крови по id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пакет найден"),
            @ApiResponse(responseCode = "400", description = "id в пути не UUID"),
            @ApiResponse(responseCode = "401", description = "Токена нет, он битый или просрочен"),
            @ApiResponse(responseCode = "404", description = "Пакета с таким id нет")
    })
    @GetMapping("/{id}")
    // имя "id" пишем явно: без родителя Spring Boot компилятор не сохраняет имена параметров
    public BloodUnitResponse get(@PathVariable("id") UUID id) {
        return BloodUnitResponse.from(service.require(new BloodUnitId(id.toString())));
    }

    @Operation(summary = "Отправить пакет на анализы (COLLECTED -> TESTED)", description = "Могут обе роли")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статус теперь TESTED"),
            @ApiResponse(responseCode = "401", description = "Токена нет, он битый или просрочен"),
            @ApiResponse(responseCode = "404", description = "Пакета с таким id нет"),
            @ApiResponse(responseCode = "409", description = "Из текущего статуса так перейти нельзя")
    })
    @PostMapping("/{id}/test")
    public BloodUnitResponse sendToTest(@PathVariable("id") UUID id, Authentication authentication) {
        return BloodUnitResponse.from(service.sendToTest(new BloodUnitId(id.toString()), authentication.getName()));
    }

    @Operation(summary = "Допустить пакет к выдаче (TESTED -> RELEASED)", description = "Могут обе роли")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статус теперь RELEASED"),
            @ApiResponse(responseCode = "401", description = "Токена нет, он битый или просрочен"),
            @ApiResponse(responseCode = "404", description = "Пакета с таким id нет"),
            @ApiResponse(responseCode = "409", description = "Из текущего статуса так перейти нельзя")
    })
    @PostMapping("/{id}/release")
    public BloodUnitResponse release(@PathVariable("id") UUID id, Authentication authentication) {
        return BloodUnitResponse.from(service.release(new BloodUnitId(id.toString()), authentication.getName()));
    }

    @Operation(summary = "Выдать пакет в больницу (RELEASED -> ISSUED)",
            description = "Закрытое действие: только CHIEF_PHYSICIAN. Лаборант получает 403, статус не меняется")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Выдан, статус теперь ISSUED"),
            @ApiResponse(responseCode = "401", description = "Токена нет, он битый или просрочен"),
            @ApiResponse(responseCode = "403", description = "Роль не подходит (нужна CHIEF_PHYSICIAN)"),
            @ApiResponse(responseCode = "404", description = "Пакета с таким id нет"),
            @ApiResponse(responseCode = "409", description = "Правила не разрешают выдачу из текущего статуса")
    })
    @PostMapping("/{id}/issue")
    public BloodUnitResponse issue(@PathVariable("id") UUID id, Authentication authentication) {
        // имя берём из токена. Поля actor или role в JSON мы не читаем вообще
        return BloodUnitResponse.from(service.issue(new BloodUnitId(id.toString()), authentication.getName()));
    }
}
