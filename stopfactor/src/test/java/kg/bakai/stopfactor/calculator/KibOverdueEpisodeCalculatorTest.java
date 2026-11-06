package kg.bakai.stopfactor.calculator;

import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;
import kg.bakai.spr.core.kib.dto.response.Contracts;
import kg.bakai.spr.core.kib.dto.response.Contracts.Contract;
import kg.bakai.spr.core.kib.dto.response.Contracts.Contract.CalendarItem;
import kg.bakai.spr.core.kib.dto.response.Contracts.Contract.PaymentCalendarList;
import kg.bakai.spr.core.kib.dto.response.Contracts.ContractList;
import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.stopfactor.calculator.KibOverdueEpisodeCalculator.OverdueHistory;
import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.OverdueEpisode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static kg.bakai.spr.core.kib.dto.enums.RoleOfClient.CoDebtor;
import static kg.bakai.spr.core.kib.dto.enums.RoleOfClient.Guarantor;
import static kg.bakai.spr.core.kib.dto.enums.RoleOfClient.MainDebtor;
import static org.assertj.core.api.Assertions.assertThat;

class KibOverdueEpisodeCalculatorTest {

    /** November 2026 is month 1, October month 2, and so on. */
    private static final LocalDate REQUEST_DAY = LocalDate.of(2026, 11, 4);
    private static final LocalDateTime REQUEST_DATE = REQUEST_DAY.atStartOfDay();

    private final KibOverdueEpisodeCalculator calculator = new KibOverdueEpisodeCalculator();

    @Nested
    class Report {

        @Test
        void noReportOrNoContractsMeansNoOverdueHistory() {
            OverdueHistory none = new OverdueHistory(List.of(), null);

            assertThat(calculator.getOverdueHistory(null, REQUEST_DATE)).isEqualTo(none);
            assertThat(calculator.getOverdueHistory(new CustomReport(), REQUEST_DATE)).isEqualTo(none);
            assertThat(calculator.getOverdueHistory(report(), REQUEST_DATE)).isEqualTo(none);
            assertThat(calculator.getOverdueHistory(new CustomReport(new Contracts(new ContractList(null)), null), REQUEST_DATE)).isEqualTo(none);
        }

        @Test
        void contractsWithoutCalendarsCannotBeRead() {
            Contract noCalendar = new Contract(MainDebtor, null, null);
            Contract emptyCalendar = new Contract(Guarantor, null, new PaymentCalendarList(List.of()));

            assertThat(calculator.getOverdueHistory(report(noCalendar, emptyCalendar), REQUEST_DATE)).isNull();
        }

        @Test
        void contractWithoutCalendarIsSkippedWhenOthersHaveOne() {
            Contract noCalendar = new Contract(MainDebtor, null, null);

            assertThat(episodes(report(noCalendar, contract(MainDebtor, repaid(2026, 10, 12)))))
                    .containsExactly(new OverdueEpisode(2, 12));
        }

        @Test
        void onlyMainDebtorAndGuarantorContractsCount() {
            List<OverdueEpisode> episodes = episodes(report(
                    contract(MainDebtor, repaid(2026, 10, 5)),
                    contract(Guarantor, repaid(2026, 10, 6)),
                    contract(CoDebtor, repaid(2026, 10, 7))));

            assertThat(episodes).extracting(OverdueEpisode::maxOverdueDays).containsExactlyInAnyOrder(5, 6);
        }

        @Test
        void onlyCoDebtorContractsMeanNoOverdueHistory() {
            assertThat(episodes(report(contract(CoDebtor, repaid(2026, 10, 30))))).isEmpty();
        }
    }

    @Nested
    class Episodes {

        @Test
        void overdueRepaidWithinTheMonthIsAnEpisode() {
            // the real report: MaxPastDueDays 18, PastDueDays 0 at the end of the month
            assertThat(episodes(repaid(2026, 7, 18), none(2026, 8))).containsExactly(new OverdueEpisode(5, 18));
        }

        @Test
        void overdueLeftAtTheEndOfTheMonthGoesOnInTheNext() {
            assertThat(episodes(open(2026, 8, 3, 3), repaid(2026, 9, 12), none(2026, 10)))
                    .containsExactly(new OverdueEpisode(3, 12));
        }

        @Test
        void overduesRepaidWithinConsecutiveMonthsAreSeparateEpisodes() {
            assertThat(episodes(repaid(2026, 4, 8), repaid(2026, 5, 18), none(2026, 6)))
                    .containsExactly(new OverdueEpisode(8, 8), new OverdueEpisode(7, 18));
        }

        @Test
        void missingMonthEndsTheEpisode() {
            assertThat(episodes(open(2026, 8, 5, 5), repaid(2026, 10, 4)))
                    .containsExactly(new OverdueEpisode(4, 5), new OverdueEpisode(2, 4));
        }

        @Test
        void overdueOpenInTheMonthOfTheRequestCountsWithTheDaysSoFar() {
            assertThat(episodes(open(2026, 10, 4, 4), open(2026, 11, 18, 18))).containsExactly(new OverdueEpisode(1, 18));
        }

        @Test
        void episodeIsInTheMonthItsMaximumWasReached() {
            assertThat(episodes(open(2026, 7, 15, 15), repaid(2026, 8, 40), none(2026, 9)))
                    .containsExactly(new OverdueEpisode(4, 40));
        }

        @Test
        void sameMaximumTwiceTakesTheLaterMonth() {
            assertThat(episodes(open(2026, 7, 9, 9), repaid(2026, 8, 9))).containsExactly(new OverdueEpisode(4, 9));
        }

        @Test
        void monthsAfterTheMonthOfTheRequestAreIgnored() {
            // the November entry is dated November 30, after the request day, and still counts
            assertThat(episodes(repaid(2026, 11, 7), repaid(2026, 12, 60))).containsExactly(new OverdueEpisode(1, 7));
        }

        @Test
        void monthsAreReadInDateOrderAsKibSendsThemNewestFirst() {
            assertThat(episodes(none(2026, 10), repaid(2026, 9, 7), open(2026, 8, 3, 3)))
                    .containsExactly(new OverdueEpisode(3, 7));
        }

        @Test
        void missingDatesAndDaysAreSkipped() {
            CalendarItem noDate = new CalendarItem(null, 50, 50);
            CalendarItem noDays = new CalendarItem(endOfMonth(2026, 8), null, null);
            CalendarItem onlyPastDueDays = new CalendarItem(endOfMonth(2026, 10), 6, null);

            assertThat(episodes(noDate, noDays, onlyPastDueDays, null)).containsExactly(new OverdueEpisode(2, 6));
        }

        @Test
        void episodesOfAllContractsAreCollected() {
            assertThat(episodes(report(
                    contract(MainDebtor, repaid(2026, 10, 5)),
                    contract(MainDebtor, repaid(2026, 3, 40)))))
                    .containsExactly(new OverdueEpisode(2, 5), new OverdueEpisode(9, 40));
        }
    }

    @Nested
    class CompleteMonths {

        @Test
        void calendarFromTheStartOfEveryContractIsComplete() {
            Contract startedInTheCalendar = new Contract(MainDebtor, LocalDate.of(2025, 3, 19).atStartOfDay(),
                    calendar(none(2024, 12), none(2026, 11)));

            assertThat(calculator.getOverdueHistory(report(startedInTheCalendar), REQUEST_DATE).completeMonths()).isNull();
        }

        @Test
        void contractOlderThanItsCalendarIsCompleteUpToTheFirstMonth() {
            Contract since2020 = new Contract(MainDebtor, LocalDate.of(2020, 1, 10).atStartOfDay(),
                    calendar(none(2024, 12), none(2026, 11)));

            // December 2024 .. November 2026
            assertThat(calculator.getOverdueHistory(report(since2020), REQUEST_DATE).completeMonths()).isEqualTo(24);
        }

        @Test
        void shortestCalendarOfTheContractsWins() {
            Contract twelveMonths = new Contract(MainDebtor, null, calendar(none(2025, 12), none(2026, 11)));
            Contract twentyFourMonths = new Contract(Guarantor, LocalDate.of(2019, 5, 1).atStartOfDay(),
                    calendar(none(2024, 12), none(2026, 11)));

            assertThat(calculator.getOverdueHistory(report(twentyFourMonths, twelveMonths), REQUEST_DATE).completeMonths())
                    .isEqualTo(12);
        }

        @Test
        void monthsAgoIsCountedInCalendarMonths() {
            YearMonth request = YearMonth.from(REQUEST_DAY);

            assertThat(KibOverdueEpisodeCalculator.monthsAgo(request, request)).isEqualTo(1);
            assertThat(KibOverdueEpisodeCalculator.monthsAgo(YearMonth.of(2026, 10), request)).isEqualTo(2);
            assertThat(KibOverdueEpisodeCalculator.monthsAgo(YearMonth.of(2026, 9), request)).isEqualTo(3);
            assertThat(KibOverdueEpisodeCalculator.monthsAgo(YearMonth.of(2026, 8), request)).isEqualTo(4);
            assertThat(KibOverdueEpisodeCalculator.monthsAgo(YearMonth.of(2024, 12), request)).isEqualTo(24);
        }
    }

    /** The contracts of a real KIB report (kib/custom-report-contracts.json, without personal data). */
    @Nested
    class RealKibReport {

        private static final LocalDateTime REPORT_DATE = LocalDateTime.of(2025, 4, 25, 14, 35, 5);

        @Test
        void reportIsReadFromTheKibJson() throws IOException {
            CustomReport report = realReport();

            List<Contract> contracts = report.contracts().contractList().contracts();
            assertThat(contracts).hasSize(6).extracting(Contract::roleOfClient).containsOnly(MainDebtor);
            assertThat(contracts.getFirst().startDate()).isEqualTo(LocalDateTime.of(2024, 3, 19, 0, 0));
            assertThat(contracts.getFirst().paymentCalendarList().paymentCalendarItems()).hasSize(24)
                    .first().isEqualTo(new CalendarItem(LocalDateTime.of(2025, 4, 30, 0, 0), 0, 0));
        }

        @Test
        void overduesRepaidWithinTheMonthShowOnlyInMaxPastDueDays() throws IOException {
            List<CalendarItem> items = realReport().contracts().contractList().contracts().stream()
                    .flatMap(contract -> contract.paymentCalendarList().paymentCalendarItems().stream())
                    .toList();

            assertThat(items).extracting(CalendarItem::pastDueDays).containsOnly(0);
            assertThat(items).extracting(CalendarItem::maxPastDueDays).contains(18, 8, 2, 1);
        }

        @Test
        void episodesOfTheRealReport() throws IOException {
            OverdueHistory history = calculator.getOverdueHistory(realReport(), REPORT_DATE);

            // April 2025 is month 1. Contract 1: May and June 2024, 2 days each.
            // Contract 6: October 2023 (2), December 2023 (1), February 2024 (8), April 2024 (8), May 2024 (18),
            // July 2024 (1) - each repaid within its month, so every month is its own episode
            assertThat(history.episodes()).containsExactly(
                    new OverdueEpisode(12, 2), new OverdueEpisode(11, 2),
                    new OverdueEpisode(19, 2), new OverdueEpisode(17, 1), new OverdueEpisode(15, 8),
                    new OverdueEpisode(13, 8), new OverdueEpisode(12, 18), new OverdueEpisode(10, 1));
            // every calendar reaches back to the start of its contract
            assertThat(history.completeMonths()).isNull();
        }

        private static CustomReport realReport() throws IOException {
            try (InputStream json = KibOverdueEpisodeCalculatorTest.class.getResourceAsStream("/kib/custom-report-contracts.json")) {
                return JsonMapper.builder().build().readValue(Objects.requireNonNull(json), CustomReport.class);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------

    private List<OverdueEpisode> episodes(CalendarItem... items) {
        return episodes(report(contract(MainDebtor, items)));
    }

    private List<OverdueEpisode> episodes(CustomReport report) {
        return calculator.getOverdueHistory(report, REQUEST_DATE).episodes();
    }

    /** A month with an overdue repaid within it, as KIB sends it: on the last day of the month. */
    private static CalendarItem repaid(int year, int month, int maxPastDueDays) {
        return new CalendarItem(endOfMonth(year, month), 0, maxPastDueDays);
    }

    /** A month whose overdue is not repaid by its end: pastDueDays left, goes on into the next month. */
    private static CalendarItem open(int year, int month, int maxPastDueDays, int pastDueDays) {
        return new CalendarItem(endOfMonth(year, month), pastDueDays, maxPastDueDays);
    }

    private static CalendarItem none(int year, int month) {
        return new CalendarItem(endOfMonth(year, month), 0, 0);
    }

    private static LocalDateTime endOfMonth(int year, int month) {
        return YearMonth.of(year, month).atEndOfMonth().atStartOfDay();
    }

    /** A contract whose calendar starts with it, so its history is complete. */
    private static Contract contract(RoleOfClient role, CalendarItem... items) {
        LocalDateTime start = Arrays.stream(items).filter(Objects::nonNull).map(CalendarItem::date).filter(Objects::nonNull)
                .min(LocalDateTime::compareTo).orElse(null);
        return new Contract(role, start, calendar(items));
    }

    private static PaymentCalendarList calendar(CalendarItem... items) {
        return new PaymentCalendarList(new ArrayList<>(Arrays.asList(items)));
    }

    private static CustomReport report(Contract... contracts) {
        return new CustomReport(new Contracts(new ContractList(new ArrayList<>(Arrays.asList(contracts)))), null);
    }
}
