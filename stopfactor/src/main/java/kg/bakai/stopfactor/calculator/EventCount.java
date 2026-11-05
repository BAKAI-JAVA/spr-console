package kg.bakai.stopfactor.calculator;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The one rule of the counting calculators: count the raw events of the credit history when SPR provided them,
 * otherwise take the legacy value of the reports if there is one for these params.
 */
final class EventCount {

    private EventCount() {
    }

    /**
     * @param rawEvents raw events, null when the history has no such list
     * @param counted   which events the stop factor counts
     * @param legacy    legacy value for these params, empty if there is none
     * @return the count, or empty (NO_DATA) without raw events and without a legacy value
     */
    static <E> Optional<CheckedValue> count(List<E> rawEvents, Predicate<E> counted, Supplier<OptionalDouble> legacy) {
        if (rawEvents != null) {
            return Optional.of(CheckedValue.ofNumber(rawEvents.stream().filter(counted).count()));
        }
        OptionalDouble value = legacy.get();
        return value.isPresent() ? Optional.of(CheckedValue.ofNumber(value.getAsDouble())) : Optional.empty();
    }
}
