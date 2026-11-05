package bloodbank.config;

import bloodbank.domain.BloodGroup;
import bloodbank.domain.BloodUnitId;
import bloodbank.domain.BloodUnitNumber;
import bloodbank.domain.BloodUnitRepository;
import bloodbank.domain.BloodUnitStatus;
import bloodbank.domain.BloodUnitView;
import bloodbank.domain.DuplicateBloodUnit;
import bloodbank.domain.HospitalNotifier;
import bloodbank.domain.MissingBloodUnit;
import bloodbank.domain.RhFactor;
import bloodbank.domain.Rule;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**D — Dependency Inversion
 * BloodUnitService зависит только от интерфейса Rule — не знает ни про TransitionRule
 * и ещё от интерфейсов BloodUnitRepository и HospitalNotifier — конкретики тоже не знает
 * S — Single Responsibility
 * Анотация **/
@Service
public class BloodUnitService {
    private final Rule rules;/**Можно присвоить только один раз.**/
    private final BloodUnitRepository units;
    private final HospitalNotifier notifier;

/**Конструктор получает правила, хранилище и порт оповещения**/
    public BloodUnitService(Rule rules, BloodUnitRepository units, HospitalNotifier notifier) {
        this.rules = rules;/** сохроняет правила **/
        this.units = units;
        this.notifier = notifier;
    }

    public BloodUnitStatus move(BloodUnitStatus from, BloodUnitStatus to) {
        rules.check(from, to);
        return to;
    }

    /** Выдать пакет: сначала правила, и только если можно — сообщаем больнице. */
    public void issue(BloodUnitNumber number, BloodUnitStatus current) {
        rules.check(current, BloodUnitStatus.ISSUED);
        notifier.notifyIssued(number.value());
    }

    /**
     * Одна транзакция на вызов: упало — ничего не сохранилось.
     * ACID: D — Durability (после выхода из метода commit, строка осталась)
     * и I — Isolation (транзакция короткая, PostgreSQL READ COMMITTED: другие видят только закоммиченное).
     */
    @Transactional
    public BloodUnitView register(BloodUnitId id, BloodUnitNumber number, String title) {
        return register(id, number, title, null, null);
    }

    /**
     * То же самое, но с группой крови и резусом. Оба необязательные: можно передать null.
     * Эту версию вызывает контроллер, а короткая выше нужна тестам лабы 3.
     */
    @Transactional
    public BloodUnitView register(BloodUnitId id, BloodUnitNumber number, String title,
                                  BloodGroup bloodGroup, RhFactor rhFactor) {
        save(id.value(), number, title, bloodGroup, rhFactor);
        // новый пакет всегда начинает со статуса COLLECTED (так же, как пишет save)
        return new BloodUnitView(id, number, BloodUnitStatus.COLLECTED, title, bloodGroup, rhFactor);
    }

    /**
     * Прочитать пакет по id. Ничего не меняет: только загружает.
     * Если пакета нет, бросаем MissingBloodUnit, а handler превратит её в 404.
     */
    public BloodUnitView require(BloodUnitId id) {
        return units.findById(id.value()).orElseThrow(() -> new MissingBloodUnit(id.toString()));
    }

    /**
     * Две вставки в ОДНОЙ транзакции: вторая падает, откатывается всё.
     * ACID: A — Atomicity. Либо обе вставки, либо ни одной (count = 0).
     * Ошибку про дубликат мы не прячем, а бросаем заново как DuplicateBloodUnit (см. save):
     * если бы просто вернулись из catch, транзакция закоммитила бы первую вставку.
     */
    @Transactional
    public void insertTwice(BloodUnitNumber number) {
        save(UUID.randomUUID(), number, "Первая вставка", null, null);
        save(UUID.randomUUID(), number, "Вторая вставка", null, null);
    }

    /**
     * Одна вставка. Репозиторий делает persist и flush, и если номер уже есть, база отказывает:
     * Spring превращает это в DataIntegrityViolationException, а мы бросаем свою DuplicateBloodUnit.
     * Это unchecked-исключение, поэтому оно вылетает из метода с @Transactional, и Spring откатывает всё.
     */
    private void save(UUID id, BloodUnitNumber number, String title, BloodGroup bloodGroup, RhFactor rhFactor) {
        try {
            units.insert(id, number.value(), BloodUnitStatus.COLLECTED.name(), title,
                    bloodGroup == null ? null : bloodGroup.name(),
                    rhFactor == null ? null : rhFactor.name());
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateBloodUnit(number.value());
        }
    }
}
