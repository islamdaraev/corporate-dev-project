package bloodbank.handler;

import bloodbank.config.BloodUnitService;
import bloodbank.domain.BloodUnitId;
import bloodbank.domain.BloodUnitNumber;
import bloodbank.domain.BloodUnitView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Один ресурс, два глагола: POST создаёт пакет, GET читает его.
 * Контроллер выбирает код ответа (201), а вставку делает сервис из лабы 3.
 * В этом классе нет EntityManager и нет сущности.
 */
@RestController
@RequestMapping("/api/v1/blood-units")
public class BloodUnitController {

    private final BloodUnitService service;

    public BloodUnitController(BloodUnitService service) {
        this.service = service;
    }

    @Operation(summary = "Зарегистрировать пакет крови")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Создан. Заголовок Location это адрес GET"),
            @ApiResponse(responseCode = "400", description = "Номер или название пустое, название длиннее 120, неверная группа крови или резус"),
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
            @ApiResponse(responseCode = "404", description = "Пакета с таким id нет")
    })
    @GetMapping("/{id}")
    // имя "id" пишем явно: без родителя Spring Boot компилятор не сохраняет имена параметров
    public BloodUnitResponse get(@PathVariable("id") UUID id) {
        return BloodUnitResponse.from(service.require(new BloodUnitId(id.toString())));
    }
}
