package bloodbank.config;

import bloodbank.domain.BloodUnitNumber;
import bloodbank.domain.BloodUnitRepository;
import bloodbank.domain.BloodUnitStatus;
import bloodbank.domain.FinalStatusRule;
import bloodbank.domain.HospitalNotifier;
import bloodbank.domain.RuleChain;
import bloodbank.domain.TransitionRule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BloodUnitServiceTest {

    // порт замокан: настоящему больничному серверу ничего не уходит
    private final HospitalNotifier notifier = mock(HospitalNotifier.class);
    private final BloodUnitRepository units = mock(BloodUnitRepository.class);
    private final BloodUnitService service = new BloodUnitService(
            new RuleChain(new TransitionRule(), new FinalStatusRule()), units, notifier);

    @Test
    void issuingReleasedUnitNotifiesHospital() {
        service.issue(new BloodUnitNumber("BU-2026-000001"), BloodUnitStatus.RELEASED);

        verify(notifier).notifyIssued("BU-2026-000001");
    }

    @Test
    void issuingCollectedUnitIsForbiddenAndHospitalIsNotNotified() {
        assertThrows(IllegalStateException.class,
                () -> service.issue(new BloodUnitNumber("BU-2026-000001"), BloodUnitStatus.COLLECTED));

        verify(notifier, never()).notifyIssued(any());
    }
}
