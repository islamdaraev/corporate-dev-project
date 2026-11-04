package bloodbank.handler;

import bloodbank.domain.BloodGroup;
import bloodbank.domain.RhFactor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Что присылает клиент в POST. Проверки (@NotBlank, @Size) живут здесь, в handler, а не в domain.
 * Тексты сообщений заданы прямо тут, чтобы не зависеть от языка компьютера.
 * bloodGroup и rhFactor необязательные: их можно не присылать.
 */
public record CreateBloodUnit(
        @Schema(description = "Человеческий номер пакета, уникальный", example = "BU-2026-000001")
        @NotBlank(message = "must not be blank") String businessKey,
        @Schema(description = "Название пакета", example = "Цельная кровь, 450 мл")
        @NotBlank(message = "must not be blank")
        @Size(max = 120, message = "size must be between 0 and 120") String title,
        @Schema(description = "Группа крови (необязательно): O это I группа, A это II, B это III, AB это IV",
                example = "A") BloodGroup bloodGroup,
        @Schema(description = "Резус-фактор (необязательно): POSITIVE это положительный (+), NEGATIVE это отрицательный (-)",
                example = "POSITIVE") RhFactor rhFactor) {
}
