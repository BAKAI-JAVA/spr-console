package kg.bakai.stopfactor.calculator;

import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;
import kg.bakai.spr.core.kib.dto.response.Contracts;
import kg.bakai.spr.core.kib.dto.response.Contracts.Contract;
import kg.bakai.spr.core.kib.dto.response.Contracts.Contract.CalendarItem;
import kg.bakai.spr.core.kib.dto.response.Contracts.Contract.PaymentCalendarList;
import kg.bakai.spr.core.kib.dto.response.Contracts.ContractList;
import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.OverdueEpisode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static kg.bakai.spr.core.kib.dto.enums.RoleOfClient.CoDebtor;
import static kg.bakai.spr.core.kib.dto.enums.RoleOfClient.Guarantor;
import static kg.bakai.spr.core.kib.dto.enums.RoleOfClient.MainDebtor;
import static org.assertj.core.api.Assertions.assertThat;

class KibOverdueEpisodeCalculatorTest {

    private static final LocalDate REQUEST_DAY = LocalDate.of(2026, 11, 4);
    private static final LocalDateTime REQUEST_DATE = REQUEST_DAY.atStartOfDay();

    private final KibOverdueEpisodeCalculator calculator = new KibOverdueEpisodeCalculator();

    @Nested
    class Report {

        @Test
        void noReportOrNoContractsMeansNoOverdueHistory() {
            assertThat(calculator.getOverdueEpisodes(null, REQUEST_DATE)).isEmpty();
            assertThat(calculator.getOverdueEpisodes(new CustomReport(), REQUEST_DATE)).isEmpty();
            assertThat(calculator.getOverdueEpisodes(report(), REQUEST_DATE)).isEmpty();
            assertThat(calculator.getOverdueEpisodes(new CustomReport(new Contracts(new ContractList(null)), null), REQUEST_DATE)).isEmpty();
        }

        @Test
        void contractsWithoutCalendarsCannotBeRead() {
            Contract noCalendar = new Contract(MainDebtor, null);
            Contract emptyCalendar = new Contract(Guarantor, new PaymentCalendarList(List.of()));

            assertThat(calculator.getOverdueEpisodes(report(noCalendar, emptyCalendar), REQUEST_DATE)).isNull();
        }

        @Test
        void contractWithoutCalendarIsSkippedWhenOthersHaveOne() {
            Contract noCalendar = new Contract(MainDebtor, null);

            assertThat(calculator.getOverdueEpisodes(report(noCalendar, contract(MainDebtor, item(2026, 10, 10, 12))), REQUEST_DATE))
                    .containsExactly(new OverdueEpisode(1, 12));
        }

        @Test
        void onlyMainDebtorAndGuarantorContractsCount() {
            List<OverdueEpisode> episodes = calculator.getOverdueEpisodes(report(
                    contract(MainDebtor, item(2026, 10, 10, 5)),
                    contract(Guarantor, item(2026, 10, 10, 6)),
                    contract(CoDebtor, item(2026, 10, 10, 7))), REQUEST_DATE);

            assertThat(episodes).extracting(OverdueEpisode::maxOverdueDays).containsExactlyInAnyOrder(5, 6);
        }

        @Test
        void onlyCoDebtorContractsMeanNoOverdueHistory() {
            assertThat(calculator.getOverdueEpisodes(report(contract(CoDebtor, item(2026, 10, 10, 30))), REQUEST_DATE)).isEmpty();
        }
    }

    @Nested
    class Episodes {

        @Test
        void runOfOverdueEntriesIsOneEpisodeWithItsMaximum() {
            assertThat(episodes(item(2026, 8, 10, 3), item(2026, 9, 10, 7), item(2026, 10, 10, 12), item(2026, 10, 25, 0)))
                    .containsExactly(new OverdueEpisode(1, 12));
        }

        @Test
        void entryWithoutOverdueSeparatesEpisodes() {
            assertThat(episodes(item(2026, 5, 10, 5), item(2026, 6, 10, 0), item(2026, 9, 10, 3), item(2026, 10, 10, 0)))
                    .containsExactly(new OverdueEpisode(6, 5), new OverdueEpisode(2, 3));
        }

        @Test
        void overdueOpenOnTheRequestDateCountsWithTheDaysSoFar() {
            assertThat(episodes(item(2026, 10, 20, 4), item(2026, 11, 3, 18))).containsExactly(new OverdueEpisode(1, 18));
        }

        @Test
        void episodeIsInTheMonthItsMaximumWasReached() {
            // maximum 20 in July, then the overdue goes down before it is paid
            assertThat(episodes(item(2026, 7, 10, 20), item(2026, 8, 10, 6), item(2026, 9, 10, 0)))
                    .containsExactly(new OverdueEpisode(4, 20));
        }

        @Test
        void sameMaximumTwiceTakesTheLaterMonth() {
            assertThat(episodes(item(2026, 7, 10, 9), item(2026, 8, 10, 9), item(2026, 9, 10, 0)))
                    .containsExactly(new OverdueEpisode(3, 9));
        }

        @Test
        void entriesAfterTheRequestDateAreIgnored() {
            assertThat(episodes(item(2026, 10, 10, 0), item(2026, 11, 5, 30), item(2026, 12, 10, 60))).isEmpty();
        }

        @Test
        void entryOnTheRequestDayIsCountedWhateverItsTime() {
            CalendarItem laterThatDay = new CalendarItem(REQUEST_DAY.atTime(15, 30), 7);

            assertThat(calculator.getOverdueEpisodes(report(contract(MainDebtor, laterThatDay)), REQUEST_DATE))
                    .containsExactly(new OverdueEpisode(1, 7));
        }

        @Test
        void entriesAreReadInDateOrder() {
            assertThat(episodes(item(2026, 10, 25, 0), item(2026, 9, 10, 7), item(2026, 8, 10, 3)))
                    .containsExactly(new OverdueEpisode(2, 7));
        }

        @Test
        void missingDatesAndDaysAreSkipped() {
            assertThat(episodes(new CalendarItem(null, 50), new CalendarItem(LocalDate.of(2026, 9, 10).atStartOfDay(), null),
                    item(2026, 10, 10, 6), null)).containsExactly(new OverdueEpisode(1, 6));
        }

        @Test
        void episodesOfAllContractsAreCollected() {
            assertThat(calculator.getOverdueEpisodes(report(
                    contract(MainDebtor, item(2026, 10, 10, 5), item(2026, 10, 20, 0)),
                    contract(MainDebtor, item(2026, 3, 10, 40), item(2026, 4, 10, 0))), REQUEST_DATE))
                    .containsExactly(new OverdueEpisode(1, 5), new OverdueEpisode(8, 40));
        }
    }

    @Nested
    class MonthsAgo {

        @Test
        void lessThanAMonthBeforeTheRequestIsMonth1() {
            assertThat(KibOverdueEpisodeCalculator.monthsAgo(REQUEST_DAY, REQUEST_DAY)).isEqualTo(1);
            assertThat(KibOverdueEpisodeCalculator.monthsAgo(LocalDate.of(2026, 10, 5), REQUEST_DAY)).isEqualTo(1);
        }

        @Test
        void fullMonthsAreCountedFromTheRequestDay() {
            assertThat(KibOverdueEpisodeCalculator.monthsAgo(LocalDate.of(2026, 10, 4), REQUEST_DAY)).isEqualTo(2);
            assertThat(KibOverdueEpisodeCalculator.monthsAgo(LocalDate.of(2026, 8, 5), REQUEST_DAY)).isEqualTo(3);
            // exactly 3 months before: the boundary between "months 1..3" and "4..6"
            assertThat(KibOverdueEpisodeCalculator.monthsAgo(LocalDate.of(2026, 8, 4), REQUEST_DAY)).isEqualTo(4);
        }
    }

    // ------------------------------------------------------------------------------------------------

    private List<OverdueEpisode> episodes(CalendarItem... items) {
        return calculator.getOverdueEpisodes(report(contract(MainDebtor, items)), REQUEST_DATE);
    }

    private static CalendarItem item(int year, int month, int day, int pastDueDays) {
        return new CalendarItem(LocalDate.of(year, month, day).atStartOfDay(), pastDueDays);
    }

    private static Contract contract(RoleOfClient role, CalendarItem... items) {
        return new Contract(role, new PaymentCalendarList(new ArrayList<>(Arrays.asList(items))));
    }

    private static CustomReport report(Contract... contracts) {
        return new CustomReport(new Contracts(new ContractList(new ArrayList<>(Arrays.asList(contracts)))), null);
    }
}
