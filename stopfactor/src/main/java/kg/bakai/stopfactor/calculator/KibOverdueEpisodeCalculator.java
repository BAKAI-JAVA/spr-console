package kg.bakai.stopfactor.calculator;

import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;
import kg.bakai.spr.core.kib.dto.response.Contracts;
import kg.bakai.spr.core.kib.dto.response.Contracts.Contract;
import kg.bakai.spr.core.kib.dto.response.Contracts.Contract.CalendarItem;
import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.OverdueEpisode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.MONTHS;

/**
 * Builds the continuous overdue episodes of a client from the monthly payment calendars of the KIB custom report:
 * the source of every OVERDUE_BUCKET_COUNT stop factor (any period and overdue days range).
 * <p>
 * A KIB calendar entry is one month: {@code MaxPastDueDays} - the largest overdue reached in the month,
 * {@code PastDueDays} - the overdue left at its end (verified on a real report: an overdue of 18 days repaid within
 * the month has MaxPastDueDays 18 and PastDueDays 0).
 * <ul>
 *     <li>only contracts where the client is the main debtor or a guarantor;</li>
 *     <li>a month with overdue (MaxPastDueDays above 0) continues the episode of the month before when that overdue
 *     was not repaid by the end of that month (its PastDueDays above 0); otherwise it starts a new episode;</li>
 *     <li>{@code maxOverdueDays} is the largest overdue of the episode, {@code monthsAgo} the month it was reached
 *     (the later one if reached twice): 1 - the month of the request, 2 - the month before, and so on;</li>
 *     <li>months after the month of the request are ignored;</li>
 *     <li>two overdues in one month cannot be told apart and count as one episode.</li>
 * </ul>
 * KIB sends 12 or 24 months of calendar. A contract that started before its first calendar month may have had
 * overdue the calendar does not show, so the history is complete only up to that month
 * ({@link OverdueHistory#completeMonths}).
 */
@Slf4j
@Component
public class KibOverdueEpisodeCalculator {

    /**
     * The overdue episodes of a client.
     *
     * @param episodes       continuous overdues of all the contracts
     * @param completeMonths the episodes are complete for months 1..N before the request; null - for every month
     */
    public record OverdueHistory(List<OverdueEpisode> episodes, Integer completeMonths) {

        static final OverdueHistory NONE = new OverdueHistory(List.of(), null);
    }

    /**
     * @return the history; without contracts an empty one (no overdue); null if the contracts have no payment
     * calendars - then the overdue stop factors use the legacy counters, and a period / range without one sends the
     * application to the underwriter
     */
    public OverdueHistory getOverdueHistory(CustomReport customReport, LocalDateTime requestDate) {
        List<Contract> contracts = clientContracts(customReport);
        if (contracts.isEmpty()) {
            return OverdueHistory.NONE;
        }
        List<Contract> withCalendar = contracts.stream().filter(contract -> !calendarItems(contract).isEmpty()).toList();
        if (withCalendar.isEmpty()) {
            log.warn("[#getOverdueHistory] Нет платежных календарей КИБ по {} договорам, эпизоды просрочки не построены",
                    contracts.size());
            return null;
        }
        if (withCalendar.size() < contracts.size()) {
            log.warn("[#getOverdueHistory] Нет платежного календаря КИБ у {} из {} договоров, их просрочки не учтены",
                    contracts.size() - withCalendar.size(), contracts.size());
        }

        YearMonth requestMonth = YearMonth.from(requestDate);
        List<OverdueEpisode> episodes = new ArrayList<>();
        Integer completeMonths = null;
        for (Contract contract : withCalendar) {
            List<CalendarItem> months = calendarMonths(contract, requestMonth);
            episodes.addAll(episodesOf(months, requestMonth));
            Integer contractMonths = completeMonths(contract, months, requestMonth);
            if (contractMonths != null && (completeMonths == null || contractMonths < completeMonths)) {
                completeMonths = contractMonths;
            }
        }
        log.debug("[#getOverdueHistory] Эпизодов просрочки: {}, полная история за месяцев: {}", episodes.size(),
                completeMonths != null ? completeMonths : "все");
        return new OverdueHistory(episodes, completeMonths);
    }

    /** Calendar entries with a date up to the month of the request, in date order. */
    private static List<CalendarItem> calendarMonths(Contract contract, YearMonth requestMonth) {
        return calendarItems(contract).stream()
                .filter(item -> item.date() != null && !YearMonth.from(item.date()).isAfter(requestMonth))
                .sorted(Comparator.comparing(CalendarItem::date))
                .toList();
    }

    private static List<OverdueEpisode> episodesOf(List<CalendarItem> months, YearMonth requestMonth) {
        List<OverdueEpisode> episodes = new ArrayList<>();
        CalendarItem previous = null;
        int maxDays = 0;
        YearMonth maxMonth = null;
        for (CalendarItem item : months) {
            int days = maxPastDueDays(item);
            if (maxMonth != null && !(days > 0 && continuesEpisode(previous, item))) {
                episodes.add(new OverdueEpisode(monthsAgo(maxMonth, requestMonth), maxDays));
                maxDays = 0;
                maxMonth = null;
            }
            if (days > 0 && days >= maxDays) {
                maxDays = days;
                maxMonth = YearMonth.from(item.date());
            }
            previous = item;
        }
        // An overdue still open in the month of the request counts with the days reached by then
        if (maxMonth != null) {
            episodes.add(new OverdueEpisode(monthsAgo(maxMonth, requestMonth), maxDays));
        }
        return episodes;
    }

    /** The overdue of the month before was not repaid by its end, so it goes on in this month. */
    private static boolean continuesEpisode(CalendarItem previous, CalendarItem item) {
        return previous != null
                && YearMonth.from(previous.date()).plusMonths(1).equals(YearMonth.from(item.date()))
                && pastDueDays(previous) > 0;
    }

    /**
     * Months 1..N the calendar of a contract shows completely: null when the calendar starts no later than the
     * contract, otherwise up to its first month.
     */
    private static Integer completeMonths(Contract contract, List<CalendarItem> months, YearMonth requestMonth) {
        if (months.isEmpty()) {
            return null;
        }
        YearMonth firstMonth = YearMonth.from(months.getFirst().date());
        if (contract.startDate() != null && !YearMonth.from(contract.startDate()).isBefore(firstMonth)) {
            return null;
        }
        return monthsAgo(firstMonth, requestMonth);
    }

    /** 1 - the month of the request, 2 - the month before it, and so on. */
    static int monthsAgo(YearMonth month, YearMonth requestMonth) {
        return (int) MONTHS.between(month, requestMonth) + 1;
    }

    /** Largest overdue of the month; an entry without MaxPastDueDays has at least its overdue at the end. */
    private static int maxPastDueDays(CalendarItem item) {
        return Math.max(Objects.requireNonNullElse(item.maxPastDueDays(), 0), pastDueDays(item));
    }

    private static int pastDueDays(CalendarItem item) {
        return Objects.requireNonNullElse(item.pastDueDays(), 0);
    }

    private static List<Contract> clientContracts(CustomReport customReport) {
        if (customReport == null) {
            return List.of();
        }
        Contracts contracts = customReport.contracts();
        if (contracts == null || contracts.contractList() == null || contracts.contractList().contracts() == null) {
            return List.of();
        }
        return contracts.contractList().contracts().stream()
                .filter(Objects::nonNull)
                .filter(contract -> contract.roleOfClient() == RoleOfClient.MainDebtor
                        || contract.roleOfClient() == RoleOfClient.Guarantor)
                .toList();
    }

    private static List<CalendarItem> calendarItems(Contract contract) {
        if (contract.paymentCalendarList() == null || contract.paymentCalendarList().paymentCalendarItems() == null) {
            return List.of();
        }
        return contract.paymentCalendarList().paymentCalendarItems().stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
