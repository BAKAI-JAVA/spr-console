package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.dto.scoring.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OverdueBucketCountCalculatorTest {

    private final OverdueBucketCountCalculator calculator = new OverdueBucketCountCalculator();

    private static Map<String, Object> bucket(int from, int to, int min, Integer max) {
        Map<String, Object> params = new HashMap<>(Map.of("periodFromMonth", from, "periodToMonth", to, "minOverdueDays", min));
        if (max != null) {
            params.put("maxOverdueDays", max);
        }
        return params;
    }

    private static StopFactorCheckRequest legacy(OverdueForStopFactor overdue) {
        StopFactorIntegerValueDto zero = new StopFactorIntegerValueDto(0, 0);
        StopFactorIntegerValueDto values = new StopFactorIntegerValueDto(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, overdue);
        return new StopFactorCheckRequest(1L, null, overdue != null ? values : zero, null);
    }

    private static StopFactorCheckRequest raw(List<CreditHistoryEvents.OverdueEpisode> episodes) {
        return new StopFactorCheckRequest(1L, null, new StopFactorIntegerValueDto(0, 0),
                new CreditHistoryEvents(episodes, null, null, null));
    }

    @Test
    void seededBucketReadsItsLegacyField() {
        OverdueForStopFactor overdue = new OverdueForStopFactor(9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3);

        assertThat(calculator.calculate(bucket(1, 3, 1, 10), legacy(overdue))).contains(CheckedValue.ofNumber(9));
        assertThat(calculator.calculate(bucket(25, 36, 91, null), legacy(overdue))).contains(CheckedValue.ofNumber(3));
    }

    @Test
    void bucketWithoutLegacyFieldHasNoDataWithoutRawHistory() {
        assertThat(calculator.calculate(bucket(1, 6, 30, null), legacy(null))).isEqualTo(Optional.empty());
    }

    @Test
    void rawEpisodesAreCountedInsidePeriodAndOverdueRangeInclusive() {
        List<CreditHistoryEvents.OverdueEpisode> episodes = List.of(
                new CreditHistoryEvents.OverdueEpisode(1, 1),
                new CreditHistoryEvents.OverdueEpisode(3, 10),
                new CreditHistoryEvents.OverdueEpisode(4, 5),
                new CreditHistoryEvents.OverdueEpisode(2, 11));

        assertThat(calculator.calculate(bucket(1, 3, 1, 10), raw(episodes))).contains(CheckedValue.ofNumber(2));
        assertThat(calculator.calculate(bucket(1, 3, 11, null), raw(episodes))).contains(CheckedValue.ofNumber(1));
    }

    @Test
    void rawHistoryTakesPrecedenceOverLegacyValues() {
        OverdueForStopFactor overdue = new OverdueForStopFactor(9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        StopFactorCheckRequest request = new StopFactorCheckRequest(1L, null,
                new StopFactorIntegerValueDto(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, overdue),
                new CreditHistoryEvents(List.of(), null, null, null));

        assertThat(calculator.calculate(bucket(1, 3, 1, 10), request)).contains(CheckedValue.ofNumber(0));
    }

    @Test
    void paramsAreNormalizedAndValidated() {
        assertThat(calculator.normalizeParams(Map.of("periodFromMonth", "1", "periodToMonth", 3.0, "minOverdueDays", 1L)))
                .isEqualTo(Map.of("periodFromMonth", 1, "periodToMonth", 3, "minOverdueDays", 1));

        assertThatThrownBy(() -> calculator.normalizeParams(Map.of("periodFromMonth", 6, "periodToMonth", 3, "minOverdueDays", 1)))
                .isInstanceOf(InvalidParamsException.class)
                .hasMessageContaining("Параметр 'periodFromMonth' должен быть <= 'periodToMonth'");
        assertThatThrownBy(() -> calculator.normalizeParams(Map.of("periodFromMonth", 1.5, "periodToMonth", 3, "foo", 1)))
                .isInstanceOf(InvalidParamsException.class)
                .hasMessageContaining("Неизвестный параметр 'foo'")
                .hasMessageContaining("Параметр 'periodFromMonth' должен быть целым числом")
                .hasMessageContaining("Параметр 'minOverdueDays' обязателен");
    }
}
