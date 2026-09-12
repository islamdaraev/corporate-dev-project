package bloodbank;

import java.util.Objects;

/**
 * Номер пакета крови, например "BU-2026-000123".
 * Пустой или отсутствующий (null) номер недопустим.
 * нет наследования
 */
public final class BloodUnitId {

    private final String value;
    /** Конструктор — код, который выполняется при new BloodUnitId(...).*/
    public BloodUnitId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Номер пакета крови не может быть пустым");
        }
        this.value = value;
    }

    public String value() {
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
        return value;
    }
}
