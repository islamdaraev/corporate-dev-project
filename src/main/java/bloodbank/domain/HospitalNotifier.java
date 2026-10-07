package bloodbank.domain;

/** Внешний порт: сообщить больнице, что пакет выдан. Реализация живёт снаружи domain. */
public interface HospitalNotifier {
    void notifyIssued(String businessKey);
}
