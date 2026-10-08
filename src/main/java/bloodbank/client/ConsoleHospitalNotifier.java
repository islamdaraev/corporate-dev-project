package bloodbank.client;

import bloodbank.domain.HospitalNotifier;
import org.springframework.stereotype.Component;

/** Пока вместо настоящего HTTP просто печатает в консоль. */
@Component
public class ConsoleHospitalNotifier implements HospitalNotifier {

    @Override
    public void notifyIssued(String businessKey) {
        System.out.println("Больнице сообщили: пакет " + businessKey + " выдан");
    }
}
