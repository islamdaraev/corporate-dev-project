package bloodbank.config;

import bloodbank.domain.BloodUnitStatus;
import bloodbank.domain.Rule;
import org.springframework.stereotype.Service;

@Service
public class BloodUnitService {

    private final Rule rules;

    public BloodUnitService(Rule rules) {
        this.rules = rules;
    }

    public BloodUnitStatus move(BloodUnitStatus from, BloodUnitStatus to) {
        rules.check(from, to);
        return to;
    }
}

