package bloodbank.domain;

import java.util.UUID;

/**
 * Что нужно сервису от хранилища. Как это сделано (JPA, SQL) — не забота domain.
 * ACID: D — Durability (после commit строка остаётся в базе)
 * и C — Consistency (UNIQUE не даёт записать один номер дважды).
 */
public interface BloodUnitRepository {

    /**
     * Записывает пакет и сразу отправляет его в базу (flush).
     * Если пакет с таким номером уже есть, база откажет, и ошибка уйдёт наверх:
     * BloodUnitService превратит её в DuplicateBloodUnit.
     */
    void insert(UUID id, String businessKey, String status, String title);

    int count(String businessKey);
}
