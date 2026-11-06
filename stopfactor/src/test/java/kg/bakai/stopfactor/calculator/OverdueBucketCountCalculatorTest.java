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

    /** Params as stored, parsed the way the check parses them. */
    private OverdueBucketCountCalculator.Bucket parsed(int from, int to, int min, Integer max) {
        return calculator.parseParams(bucket(from, to, min, max));
    }

    /** No raw history: the SPR counters are the only source. */
    private static StopFactorCheckRequest legacy(OverdueForStopFactor overdue) {
        return new StopFactorCheckRequest(1L, null, values(overdue), null, List.of());
    }

    private static ReportValues values(OverdueForStopFactor overdue) {
        LegacyCounters legacy = new LegacyCounters(0, 0, 0, 0, 0, 0, LegacyOverdueCounters.from(overdue));
        return new ReportValues(0, 0, 0, 0, 0, legacy);
    }

    private static StopFactorCheckRequest raw(List<CreditHistoryEvents.OverdueEpisode> episodes) {
        return new StopFactorCheckRequest(1L, null, ReportValues.withoutKibReport(0, 0),
                CreditHistoryEvents.ofOverdueEpisodes(episodes), List.of());
    }

    @Test
    void seededBucketReadsItsLegacyField() {
        OverdueForStopFactor overdue = new OverdueForStopFactor(9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3);

        assertThat(calculator.calculate(parsed(1, 3, 1, 10), legacy(overdue))).contains(CheckedValue.ofNumber(9));
        assertThat(calculator.calculate(parsed(25, 36, 91, null), legacy(overdue))).contains(CheckedValue.ofNumber(3));
    }

    @Test
    void bucketWithoutLegacyFieldHasNoDataWithoutRawHistory() {
        assertThat(calculator.calculate(parsed(1, 6, 30, null), legacy(null))).isEqualTo(Optional.empty());
    }

    @Test
    void rawEpisodesAreCountedInsidePeriodAndOverdueRangeInclusive() {
        List<CreditHistoryEvents.OverdueEpisode> episodes = List.of(
                new CreditHistoryEvents.OverdueEpisode(1, 1),
                new CreditHistoryEvents.OverdueEpisode(3, 10),
                new CreditHistoryEvents.OverdueEpisode(4, 5),
                new CreditHistoryEvents.OverdueEpisode(2, 11));

        assertThat(calculator.calculate(parsed(1, 3, 1, 10), raw(episodes))).contains(CheckedValue.ofNumber(2));
        assertThat(calculator.calculate(parsed(1, 3, 11, null), raw(episodes))).contains(CheckedValue.ofNumber(1));
    }

    @Test
    void rawHistoryTakesPrecedenceOverLegacyValues() {
        OverdueForStopFactor overdue = new OverdueForStopFactor(9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        StopFactorCheckRequest request = new StopFactorCheckRequest(1L, null,
                values(overdue),
                CreditHistoryEvents.ofOverdueEpisodes(List.of()), List.of());

        assertThat(calculator.calculate(parsed(1, 3, 1, 10), request)).contains(CheckedValue.ofNumber(0));
    }

    @Test
    void periodLongerThanTheCompleteHistoryUsesTheLegacyCounter() {
        // the calendars cover 24 months: no episodes are known in months 25..36
        OverdueForStopFactor overdue = new OverdueForStopFactor(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4);
        StopFactorCheckRequest request = new StopFactorCheckRequest(1L, null, values(overdue),
                new CreditHistoryEvents(List.of(new CreditHistoryEvents.OverdueEpisode(30, 120)), 24, null, null, null), List.of());

        assertThat(calculator.calculate(parsed(1, 24, 91, null), request)).contains(CheckedValue.ofNumber(0));
        assertThat(calculator.calculate(parsed(25, 36, 91, null), request)).contains(CheckedValue.ofNumber(4));
        assertThat(calculator.calculate(parsed(25, 36, 31, 60), request)).isEqualTo(Optional.empty());
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
