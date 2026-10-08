package bloodbank.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * класс для хранения и проверки
 * Внутренний идентификатор пакета крови — случайный UUID.
 * Его создаёт программа (newId), человек его не видит.
 * нет наследования
 * ACID: C — Consistency. null, пусто и не-UUID не пройдут, в базу плохой id не попадёт.
 */
public final class BloodUnitId {

    private final UUID value;
    /** Конструктор — код, который выполняется при new BloodUnitId(...).*/
    public BloodUnitId(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Идентификатор пакета крови не может быть пустым");
        }
        this.value = UUID.fromString(text); // не UUID — бросит IllegalArgumentException
    }

    public static BloodUnitId newId() {
        return new BloodUnitId(UUID.randomUUID().toString());
    }

    public UUID value() {
        return value;
    }
/**переопределение метода*/
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BloodUnitId other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
