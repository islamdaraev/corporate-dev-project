package bloodbank.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Что нужно сервису от хранилища. Как это сделано (JPA, SQL) — не забота domain.
 * ACID: D — Durability (после commit строка остаётся в базе)
 * и C — Consistency (UNIQUE не даёт записать один номер дважды).
 */
public interface BloodUnitRepository {

    /**
     * Записывает пакет и сразу отправляет его в базу (flush).
     * bloodGroup и rhFactor необязательные: передаём слова группы и резуса или null.
     * Если пакет с таким номером уже есть, база откажет, и ошибка уйдёт наверх:
     * BloodUnitService превратит её в DuplicateBloodUnit.
     */
    void insert(UUID id, String businessKey, String status, String title, String bloodGroup, String rhFactor);

    int count(String businessKey);

    /**
     * Находит пакет по id (UUID, а не по человеческому номеру).
     * Пустой Optional значит, что такой строки нет. Сущность наружу не отдаём: только BloodUnitView.
     */
    Optional<BloodUnitView> findById(UUID id);

    /**
     * Меняет статус, но только если сейчас он равен from. Возвращает, сколько строк изменилось (0 или 1).
     * 0 значит, что кто-то уже поменял статус, и мы ничего не затёрли.
     */
    int updateStatus(UUID id, String from, String to);
}
