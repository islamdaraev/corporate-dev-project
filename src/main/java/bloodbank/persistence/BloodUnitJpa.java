package bloodbank.persistence;

import bloodbank.domain.BloodGroup;
import bloodbank.domain.BloodUnitId;
import bloodbank.domain.BloodUnitNumber;
import bloodbank.domain.BloodUnitRepository;
import bloodbank.domain.BloodUnitStatus;
import bloodbank.domain.BloodUnitView;
import bloodbank.domain.RhFactor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Только работа с базой через JPA: записать пакет, посчитать пакеты по номеру, найти по id. */
@Repository
public class BloodUnitJpa implements BloodUnitRepository {

    private final EntityManager em;

    public BloodUnitJpa(EntityManager em) {
        this.em = em;
    }

    @Override
    public void insert(UUID id, String businessKey, String status, String title,
                       String bloodGroup, String rhFactor) {
        em.persist(new BloodUnitEntity(id, businessKey, status, title, bloodGroup, rhFactor));
        // persist только запоминает строку, INSERT уходит в базу на flush.
        // Без flush ошибка про дубликат вылетела бы позже, уже после catch в сервисе.
        em.flush();
    }

    @Override
    public int count(String businessKey) {
        Long n = em.createQuery(
                        "select count(b) from BloodUnitEntity b where b.businessKey = :key", Long.class)
                .setParameter("key", businessKey)
                .getSingleResult();
        return n.intValue();
    }

    /** Читает пакет по id. Пустой Optional значит, что такой строки нет. */
    @Override
    public Optional<BloodUnitView> findById(UUID id) {
        List<Object[]> rows = em.createQuery(
                        "select b.id, b.businessKey, b.status, b.title, b.bloodGroup, b.rhFactor "
                                + "from BloodUnitEntity b where b.id = :id",
                        Object[].class)
                .setParameter("id", id)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        return Optional.of(new BloodUnitView(
                new BloodUnitId(row[0].toString()),
                new BloodUnitNumber((String) row[1]),
                BloodUnitStatus.valueOf((String) row[2]),
                (String) row[3],
                row[4] == null ? null : BloodGroup.valueOf((String) row[4]),
                row[5] == null ? null : RhFactor.valueOf((String) row[5])));
    }
}
