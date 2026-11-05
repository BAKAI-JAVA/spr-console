package kg.bakai.stopfactor.evaluation;

import kg.bakai.spr.core.kib.dto.enums.LoanStatus;
import kg.bakai.stopfactor.calculator.CheckedValue;
import kg.bakai.stopfactor.entity.StopFactorCondition;
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
 *     <li>a number condition (INTEGER, DOUBLE) holds the highest allowed value and fires when the value is greater;</li>
 *     <li>a status condition (ARRAY, or STRING for one status) fires when a loan of the client has one of its
 *     statuses; the checked value is that status, or {@link #NO_MATCHES}.</li>
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
        return switch (condition.getValueType()) {
            case INTEGER, DOUBLE -> {
                double number = number(value);
                boolean fires = number > Double.parseDouble(condition.getValue().trim());
                yield new Outcome(fires ? condition.getResult() : APPROVED, String.valueOf(number));
            }
            case ARRAY, STRING -> {
                Set<String> statuses = statuses(condition.getValue());
                yield value.values().stream()
                        .filter(statuses::contains)
                        .findFirst()
                        .map(status -> new Outcome(condition.getResult(), status))
                        .orElseGet(() -> new Outcome(APPROVED, NO_MATCHES));
            }
        };
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
