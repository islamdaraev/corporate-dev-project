package bloodbank.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Одна строка таблицы blood_unit. Только названия колонок, никаких правил.
 * Правила переходов остаются в domain, а таблицу создаёт Flyway (V1), не Hibernate.
 */
@Entity // аннотация является сущностью JPA
@Table(name = "blood_unit")
public class BloodUnitEntity {

    // id берём из BloodUnitId.newId(), поэтому @GeneratedValue нет
    @Id
    private UUID id;

    @Column(name = "business_key", nullable = false, unique = true)
    private String businessKey;

    // статус храним текстом: COLLECTED, TESTED, RELEASED, ISSUED, DISCARDED (как в CHECK)
    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String title;

    // группа крови и резус: необязательные, поэтому без nullable = false
    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(name = "rh_factor")
    private String rhFactor;

    // время ставит база (DEFAULT now()), поэтому Java его не пишет
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Нужен самому JPA, сами мы его не вызываем. доступен внутри пакета и наследникам класса */
    protected BloodUnitEntity() {
    }

    public BloodUnitEntity(UUID id, String businessKey, String status, String title,
                           String bloodGroup, String rhFactor) {
        this.id = id;
        this.businessKey = businessKey;
        this.status = status;
        this.title = title;
        this.bloodGroup = bloodGroup;
        this.rhFactor = rhFactor;
    }
}