package bloodbank.config;

import bloodbank.domain.BloodGroup;
import bloodbank.domain.BloodUnitId;
import bloodbank.domain.BloodUnitNumber;
import bloodbank.domain.BloodUnitRepository;
import bloodbank.domain.BloodUnitStatus;
import bloodbank.domain.BloodUnitView;
import bloodbank.domain.FinalStatusRule;
import bloodbank.domain.HospitalNotifier;
import bloodbank.domain.MissingBloodUnit;
import bloodbank.domain.RhFactor;
import bloodbank.domain.RuleChain;
import bloodbank.domain.TransitionRule;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void registerReturnsTheNewUnitWithStatusCollected() {
        BloodUnitId id = BloodUnitId.newId();
        BloodUnitNumber number = new BloodUnitNumber("BU-2026-000001");

        BloodUnitView created = service.register(id, number, "Первая заявка");

        assertEquals(new BloodUnitView(id, number, BloodUnitStatus.COLLECTED, "Первая заявка", null, null), created);
        verify(units).insert(id.value(), "BU-2026-000001", "COLLECTED", "Первая заявка", null, null);
    }

    @Test
    void registerWithGroupAndRhPassesTheirWordsToTheRepository() {
        BloodUnitId id = BloodUnitId.newId();
        BloodUnitNumber number = new BloodUnitNumber("BU-2026-000001");

        BloodUnitView created = service.register(id, number, "Цельная кровь", BloodGroup.AB, RhFactor.NEGATIVE);

        assertEquals(BloodGroup.AB, created.bloodGroup());
        assertEquals(RhFactor.NEGATIVE, created.rhFactor());
        verify(units).insert(id.value(), "BU-2026-000001", "COLLECTED", "Цельная кровь", "AB", "NEGATIVE");
    }

    @Test
    void requireReturnsTheFoundUnit() {
        BloodUnitId id = BloodUnitId.newId();
        BloodUnitView found = new BloodUnitView(id, new BloodUnitNumber("BU-2026-000001"),
                BloodUnitStatus.COLLECTED, "Первая заявка", null, null);
        when(units.findById(id.value())).thenReturn(Optional.of(found));

        assertEquals(found, service.require(id));
    }

    @Test
    void requireThrowsMissingBloodUnitWhenThereIsNoRow() {
        BloodUnitId id = BloodUnitId.newId();
        when(units.findById(id.value())).thenReturn(Optional.empty());

        assertThrows(MissingBloodUnit.class, () -> service.require(id));
    }
}
