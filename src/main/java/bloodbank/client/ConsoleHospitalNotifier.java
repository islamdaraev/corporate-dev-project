package bloodbank.client;

import bloodbank.config.DownstreamProperties;
import bloodbank.domain.HospitalNotifier;
import org.springframework.stereotype.Component;

/** Пока вместо настоящего HTTP просто печатает в консоль. Адрес берёт из настроек. */
@Component
public class ConsoleHospitalNotifier implements HospitalNotifier {

    private final DownstreamProperties downstream;

    public ConsoleHospitalNotifier(DownstreamProperties downstream) {
        this.downstream = downstream;
    }

    @Override
    public void notifyIssued(String businessKey) {
        System.out.println("Больнице (" + downstream.baseUrl() + ") сообщили: пакет " + businessKey + " выдан");
    }
}
