package kg.bakai.stopfactor.dto.scoring;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.Source.KIB;
import static kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.Source.OB;
import static org.assertj.core.api.Assertions.assertThat;

class LegacyCountersTest {

    private final LegacyCounters counters = new LegacyCounters(1, 2, 3, 4, 7, 30, LegacyOverdueCounters.NONE);

    @Test
    void restructuringsHaveTwoPeriodsPerSource() {
        assertThat(counters.restructurings(KIB, 1, 12)).isEqualTo(OptionalDouble.of(1));
        assertThat(counters.restructurings(KIB, 13, 24)).isEqualTo(OptionalDouble.of(2));
        assertThat(counters.restructurings(OB, 1, 12)).isEqualTo(OptionalDouble.of(3));
        assertThat(counters.restructurings(OB, 13, 24)).isEqualTo(OptionalDouble.of(4));
        assertThat(counters.restructurings(KIB, 1, 24)).isEmpty();
        assertThat(counters.restructurings(OB, 1, 6)).isEmpty();
    }

    @Test
    void recentEventsHaveOnePeriod() {
        assertThat(counters.bureauRequests(7)).isEqualTo(OptionalDouble.of(7));
        assertThat(counters.bureauRequests(30)).isEmpty();
        assertThat(counters.newCredits(30)).isEqualTo(OptionalDouble.of(30));
        assertThat(counters.newCredits(7)).isEmpty();
    }

    @Test
    void everySprOverdueCounterIsReadByItsStopFactorParams() {
        // every SPR counter has a different value, so a wrong pairing shows another number
        LegacyOverdueCounters overdue = LegacyOverdueCounters.from(
                new OverdueForStopFactor(101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117));

        assertThat(overdue.count(1, 3, 1, 10)).isEqualTo(OptionalDouble.of(101));     // SPR "1-15 days"
        assertThat(overdue.count(1, 3, 11, 15)).isEqualTo(OptionalDouble.of(102));    // SPR "16-30 days"
        assertThat(overdue.count(1, 3, 16, null)).isEqualTo(OptionalDouble.of(103));  // SPR "31+ days"
        assertThat(overdue.count(4, 6, 1, 10)).isEqualTo(OptionalDouble.of(104));
        assertThat(overdue.count(4, 6, 11, 15)).isEqualTo(OptionalDouble.of(105));
        assertThat(overdue.count(4, 6, 16, null)).isEqualTo(OptionalDouble.of(106));
        assertThat(overdue.count(7, 12, 1, 4)).isEqualTo(OptionalDouble.of(107));
        assertThat(overdue.count(7, 12, 5, 14)).isEqualTo(OptionalDouble.of(108));
        assertThat(overdue.count(7, 12, 15, 20)).isEqualTo(OptionalDouble.of(109));
        assertThat(overdue.count(7, 12, 21, null)).isEqualTo(OptionalDouble.of(110));
        assertThat(overdue.count(13, 24, 1, 7)).isEqualTo(OptionalDouble.of(111));
        assertThat(overdue.count(13, 24, 8, 20)).isEqualTo(OptionalDouble.of(112));
        assertThat(overdue.count(13, 24, 21, 30)).isEqualTo(OptionalDouble.of(113));
        assertThat(overdue.count(13, 24, 31, null)).isEqualTo(OptionalDouble.of(114));
        assertThat(overdue.count(25, 36, 1, 29)).isEqualTo(OptionalDouble.of(115));
        assertThat(overdue.count(25, 36, 30, 90)).isEqualTo(OptionalDouble.of(116));
        assertThat(overdue.count(25, 36, 91, null)).isEqualTo(OptionalDouble.of(117));
    }

    @Test
    void overdueParamsWithoutACounterHaveNoData() {
        assertThat(LegacyOverdueCounters.NONE.count(1, 3, 1, 15)).isEmpty();      // the SPR name, not the stop factor params
        assertThat(LegacyOverdueCounters.NONE.count(1, 12, 30, null)).isEmpty();  // a 2026 rule
        assertThat(LegacyOverdueCounters.NONE.count(1, 3, 1, 10)).isEqualTo(OptionalDouble.of(0));
    }

    @Test
    void noSprCountersAreZeros() {
        assertThat(LegacyOverdueCounters.from(null)).isEqualTo(LegacyOverdueCounters.NONE);
        assertThat(LegacyOverdueCounters.from(OverdueForStopFactor.NONE)).isEqualTo(LegacyOverdueCounters.NONE);
    }
}
