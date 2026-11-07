package kg.bakai.stopfactor.condition;

import kg.bakai.spr.core.kib.dto.enums.LoanStatus;
import kg.bakai.stopfactor.calculator.CheckedValue;
import kg.bakai.stopfactor.entity.StopFactorCondition;
import kg.bakai.stopfactor.enums.ConditionOperator;
import kg.bakai.stopfactor.enums.StopFactorStatus;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static kg.bakai.stopfactor.enums.StopFactorStatus.APPROVED;
import static kg.bakai.stopfactor.enums.StopFactorStatus.REJECTED;
import static kg.bakai.stopfactor.enums.StopFactorStatus.UNDERWRITER;

/**
 * Applies the conditions of a stop factor to its measured value: the REJECTED condition first, the UNDERWRITER
 * condition only if it did not reject.
 * <ul>
 *     <li>a number condition fires when its {@link ConditionOperator} holds for the value
 *     ({@code >, >=, <, <=, =}, RANGE {@code value < x <= value_to}, BETWEEN {@code value <= x <= value_to});</li>
 *     <li>{@link ConditionOperator#IN}: fires when a loan of the client has one of the statuses; the checked value
 *     is that status, or {@link #NO_MATCHES}.</li>
 * </ul>
 */
@Slf4j
public final class ConditionEvaluator {

    /** Checked value of a status condition when no loan of the client has one of its statuses. */
    public static final String NO_MATCHES = "Нет совпадений";

    private static final List<StopFactorStatus> ORDER = List.of(REJECTED, UNDERWRITER);
    private static final Set<String> LOAN_STATUSES = Arrays.stream(LoanStatus.values()).map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    private ConditionEvaluator() {
    }

    public record Outcome(StopFactorStatus result, String checkedValue) {
    }

    public static Outcome apply(CheckedValue value, List<StopFactorCondition> conditions) {
        Outcome outcome = new Outcome(APPROVED, String.valueOf(number(value)));
        for (StopFactorStatus result : ORDER) {
            Optional<StopFactorCondition> condition = conditions.stream().filter(c -> c.getResult() == result).findFirst();
            if (condition.isPresent()) {
                outcome = test(condition.get(), value);
                if (outcome.result() == REJECTED) {
                    break;
                }
            }
        }
        return outcome;
    }

    private static Outcome test(StopFactorCondition condition, CheckedValue value) {
        return switch (condition.getOperator()) {
            case IN -> statusOutcome(condition, value);
            default -> {
                double number = number(value);
                double valueTo = condition.getOperator().hasValueTo() ? Double.parseDouble(condition.getValueTo().trim()) : Double.NaN;
                boolean fires = condition.getOperator().test(number, Double.parseDouble(condition.getValue().trim()), valueTo);
                yield new Outcome(fires ? condition.getResult() : APPROVED, String.valueOf(number));
            }
        };
    }

    private static Outcome statusOutcome(StopFactorCondition condition, CheckedValue value) {
        Set<String> statuses = statuses(condition.getValue());
        return value.values().stream()
                .filter(statuses::contains)
                .findFirst()
                .map(status -> new Outcome(condition.getResult(), status))
                .orElseGet(() -> new Outcome(APPROVED, NO_MATCHES));
    }

    /** The number of a value; a list value (loan statuses) counts as 0, as in legacy. */
    private static double number(CheckedValue value) {
        return value.isNumber() ? value.number() : 0;
    }

    /** A status unknown to LoanStatus is skipped with a warning; legacy LoanStatus.valueOf failed the whole check. */
    private static Set<String> statuses(String conditionValue) {
        Set<String> statuses = new HashSet<>();
        for (String item : conditionValue.split(",")) {
            String name = item.trim();
            if (LOAN_STATUSES.contains(name)) {
                statuses.add(name);
            } else if (!name.isEmpty()) {
                log.warn("Неизвестный статус кредита '{}' в условии стоп-фактора, пропущен", name);
            }
        }
        return statuses;
    }
}
