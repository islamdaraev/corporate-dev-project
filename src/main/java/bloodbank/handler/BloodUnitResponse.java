package bloodbank.handler;

import bloodbank.domain.BloodUnitView;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * Что получает клиент: id, номер, статус, название и, если были указаны, группа крови и резус.
 * Пустые поля в JSON не попадают (@JsonInclude): старый клиент их просто не увидит.
 * Сущность BloodUnitEntity в JSON не попадает: иначе форма таблицы стала бы публичной.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BloodUnitResponse(UUID id, String businessKey, String status, String title,
                                String bloodGroup, String rhFactor) {

    public static BloodUnitResponse from(BloodUnitView view) {
        return new BloodUnitResponse(
                view.id().value(),
                view.number().value(),
                view.status().name(),
                view.title(),
                view.bloodGroup() == null ? null : view.bloodGroup().name(),
                view.rhFactor() == null ? null : view.rhFactor().name());
    }
}
