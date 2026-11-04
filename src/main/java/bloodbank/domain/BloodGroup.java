package bloodbank.domain;

/**
 * Группа крови по системе AB0. Слова те же, что в CHECK таблицы (V2).
 * Как говорят цифрами: O это I группа, A это II, B это III, AB это IV.
 * В API и в базе храним буквы: они международные и не путаются, как цифры.
 */
public enum BloodGroup {
    O, A, B, AB
}
