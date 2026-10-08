package bloodbank.domain;

import java.util.Objects;

/**
 * Человеческий номер пакета крови, например "BU-2026-000123".
 * В базе это business_key: уникальный, его видят люди.
 * ACID: C — Consistency. null и пусто не пройдут здесь, а UNIQUE NOT NULL в базе — второй барьер.
 * I — Isolation: если двое вставят один номер, выиграет UNIQUE, второй получит DuplicateBloodUnit.
 */
public final class BloodUnitNumber {

    private final String value;

    public BloodUnitNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Номер пакета крови не может быть пустым");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BloodUnitNumber other)) return false;
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
