package bloodbank.persistence;

import bloodbank.domain.BloodUnitRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Только работа с базой через JPA: записать пакет и посчитать пакеты по номеру. */
@Repository
public class BloodUnitJpa implements BloodUnitRepository {

    private final EntityManager em;

    public BloodUnitJpa(EntityManager em) {
        this.em = em;
    }

    @Override
    public void insert(UUID id, String businessKey, String status, String title) {
        em.persist(new BloodUnitEntity(id, businessKey, status, title));
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
}
