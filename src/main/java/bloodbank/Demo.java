package bloodbank;

public class Demo {

    public static void main(String[] args) {
        BloodUnitPolicy policy = new BloodUnitPolicy();

        BloodUnitId id = new BloodUnitId("BU-2026-000123");
        System.out.println("Пакет: " + id);

        BloodUnitStatus s = BloodUnitStatus.COLLECTED;
        System.out.println("Старт:  " + s);

        s = policy.move(s, BloodUnitStatus.TESTED);
        System.out.println("  -> " + s + "   (отправили на анализы)");
        s = policy.move(s, BloodUnitStatus.RELEASED);
        System.out.println("  -> " + s + " (анализы чистые, годен)");
        s = policy.move(s, BloodUnitStatus.ISSUED);
        System.out.println("  -> " + s + "   (выдали в больницу)");

        System.out.println();
        System.out.println("Проверка запрещённых действий:");

        try {
            policy.move(BloodUnitStatus.COLLECTED, BloodUnitStatus.ISSUED);
        } catch (IllegalStateException e) {
            System.out.println("поймали: " + e.getMessage());
        }
        try {
            policy.move(BloodUnitStatus.ISSUED, BloodUnitStatus.RELEASED);
        } catch (IllegalStateException e) {
            System.out.println(" поймали: " + e.getMessage());
        }
        try {
            new BloodUnitId("   ");
        } catch (IllegalArgumentException e) {
            System.out.println(" поймали: " + e.getMessage());
        }

        System.out.println();
        System.out.println("Всё отработало как надо.");
    }
}
