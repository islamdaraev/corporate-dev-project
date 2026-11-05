package bloodbank.domain;

/**
 * Как пакет выглядит для вызывающего: id, номер, статус, название.
 * Группа крови и резус необязательные: если их не передали, они null.
 * Это обычная запись Java из domain. Сущность из persistence наружу не выходит,
 * handler превращает этот вид в JSON-ответ.
 */
public record BloodUnitView(BloodUnitId id, BloodUnitNumber number, BloodUnitStatus status, String title,
                            BloodGroup bloodGroup, RhFactor rhFactor) {
}
