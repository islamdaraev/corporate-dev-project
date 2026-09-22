package bloodbank.domain;

public interface Rule {
    void check(BloodUnitStatus from, BloodUnitStatus to);
}
