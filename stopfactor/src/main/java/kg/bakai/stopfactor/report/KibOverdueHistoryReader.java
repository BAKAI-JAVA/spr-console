package kg.bakai.stopfactor.report;

import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;
import kg.bakai.spr.core.kib.dto.response.Contracts.Contract.CalendarItem;
import kg.bakai.spr.core.kib.dto.response.Contracts.Contract;
import kg.bakai.spr.core.kib.dto.response.Contracts;
import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.stopfactor.report.CreditHistoryEvents.OverdueMonth;

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
 * The months with overdue of a client, from the payment calendars of the KIB custom report: the source of every
 * OVERDUE_BUCKET_COUNT stop factor (any period and overdue days range).
 * <p>
 * Counts as the KIB module does for the legacy counters (KibOverdueCalculator), so a stop factor with the params of
 * a legacy counter gets exactly that counter:
 * <ul>
 *     <li>only contracts where the client is the <b>main debtor</b> (not guarantor, not co-debtor);</li>
 *     <li>every calendar month with {@code MaxPastDueDays} of 1 and more is one overdue month with that many days -
 *     an overdue that goes on into the next month counts in both months;</li>
 *     <li>month 1 is the month of the request, 2 - the month before, and so on; months after it are ignored.</li>
 * </ul>
 * KIB sends 12 or 24 months of calendar. A contract that started before its first calendar month may have had
 * overdue the calendar does not show, so the history is complete only up to that month
 * ({@link OverdueHistory#completeMonths}).
 */
@Slf4j
@Component
public class KibOverdueHistoryReader {

    /**
     * The overdue months of a client.
     *
     * @param months         months with overdue of all the main debtor contracts
     * @param completeMonths the history is complete for months 1..N before the request; null - for every month
     */
    public record OverdueHistory(List<OverdueMonth> months, Integer completeMonths) {

        static final OverdueHistory NONE = new OverdueHistory(List.of(), null);
    }

    /**
     * @return the history; without main debtor contracts an empty one (no overdue); null if the contracts have no
     * payment calendars - then the overdue stop factors use the legacy counters, and a period / range without one
     * sends the application to the underwriter
     */
    public OverdueHistory read(CustomReport customReport, LocalDateTime requestDate) {
        List<Contract> contracts = mainDebtorContracts(customReport);
        if (contracts.isEmpty()) {
            return OverdueHistory.NONE;
        }
        List<Contract> withCalendar = contracts.stream().filter(contract -> !calendarItems(contract).isEmpty()).toList();
        if (withCalendar.isEmpty()) {
            log.warn("[#read] Нет платежных календарей КИБ по {} договорам, месяцы просрочки не найдены",
                    contracts.size());
            return null;
        }
        if (withCalendar.size() < contracts.size()) {
            log.warn("[#read] Нет платежного календаря КИБ у {} из {} договоров, их просрочки не учтены",
                    contracts.size() - withCalendar.size(), contracts.size());
        }

        YearMonth requestMonth = YearMonth.from(requestDate);
        List<OverdueMonth> months = new ArrayList<>();
        Integer completeMonths = null;
        for (Contract contract : withCalendar) {
            List<CalendarItem> calendar = calendarUpTo(contract, requestMonth);
            for (CalendarItem item : calendar) {
                int days = maxPastDueDays(item);
                if (days > 0) {
                    months.add(new OverdueMonth(monthsAgo(YearMonth.from(item.date()), requestMonth), days));
                }
            }
            Integer contractMonths = completeMonths(contract, calendar, requestMonth);
            if (contractMonths != null && (completeMonths == null || contractMonths < completeMonths)) {
                completeMonths = contractMonths;
            }
        }
        log.debug("[#read] Месяцев с просрочкой: {}, полная история за месяцев: {}", months.size(),
                completeMonths != null ? completeMonths : "все");
        return new OverdueHistory(months, completeMonths);
    }

    /** Calendar entries with a date up to the month of the request, in date order. */
    private static List<CalendarItem> calendarUpTo(Contract contract, YearMonth requestMonth) {
        return calendarItems(contract).stream()
                .filter(item -> item.date() != null && !YearMonth.from(item.date()).isAfter(requestMonth))
                .sorted(Comparator.comparing(CalendarItem::date))
                .toList();
    }

    /**
     * Months 1..N the calendar of a contract shows completely: null when the calendar starts no later than the
     * contract, otherwise up to its first month.
     */
    private static Integer completeMonths(Contract contract, List<CalendarItem> calendar, YearMonth requestMonth) {
        if (calendar.isEmpty()) {
            return null;
        }
        YearMonth firstMonth = YearMonth.from(calendar.getFirst().date());
        if (contract.startDate() != null && !YearMonth.from(contract.startDate()).isBefore(firstMonth)) {
            return null;
        }
        return monthsAgo(firstMonth, requestMonth);
    }

    /** 1 - the month of the request, 2 - the month before it, and so on (as KibOverdueCalculator). */
    static int monthsAgo(YearMonth month, YearMonth requestMonth) {
        return (int) MONTHS.between(month, requestMonth) + 1;
    }

    /** Largest overdue of the month; an entry without MaxPastDueDays has at least its overdue at the end. */
    private static int maxPastDueDays(CalendarItem item) {
        return Math.max(Objects.requireNonNullElse(item.maxPastDueDays(), 0), Objects.requireNonNullElse(item.pastDueDays(), 0));
    }

    private static List<Contract> mainDebtorContracts(CustomReport customReport) {
        if (customReport == null) {
            return List.of();
        }
        Contracts contracts = customReport.contracts();
        if (contracts == null || contracts.contractList() == null || contracts.contractList().contracts() == null) {
            return List.of();
        }
        return contracts.contractList().contracts().stream()
                .filter(Objects::nonNull)
                .filter(contract -> contract.roleOfClient() == RoleOfClient.MainDebtor)
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
