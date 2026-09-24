package bloodbank.domain;

/** только «вызови первое, потом второе», сам ничего не проверяет.
 * Соединяет несколько правил в одно: проверяет по очереди первое и второе.
 * Если любое из них бросит исключение — цепочка останавливается сразу.
 */
public final class RuleChain implements Rule {

    private final Rule first;
    private final Rule second;

    public RuleChain(Rule first, Rule second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public void check(BloodUnitStatus from, BloodUnitStatus to) {
        first.check(from, to);
        second.check(from, to);
    }
}
