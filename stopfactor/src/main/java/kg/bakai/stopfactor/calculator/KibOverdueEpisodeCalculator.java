package kg.bakai.stopfactor.calculator;

import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;
import kg.bakai.spr.core.kib.dto.response.Contracts;
import kg.bakai.spr.core.kib.dto.response.Contracts.Contract;
import kg.bakai.spr.core.kib.dto.response.Contracts.Contract.CalendarItem;
import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.OverdueEpisode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.MONTHS;

/**
 * Builds the continuous overdue episodes of a client from the payment calendars of the KIB custom report:
 * the source of every OVERDUE_BUCKET_COUNT stop factor (any period and overdue days range).
 * <ul>
 *     <li>only contracts where the client is the main debtor or a guarantor;</li>
 *     <li>an episode is a run of calendar entries with overdue ({@link #continuesEpisode}), ended by an entry
 *     without overdue or still open on the request date;</li>
 *     <li>{@code maxOverdueDays} is the largest overdue of the run, {@code monthsAgo} the month it was reached
 *     (the latest one if reached more than once): 1 - less than a month before the request, 2 - one to two
 *     months, and so on;</li>
 *     <li>entries after the request date are ignored.</li>
 * </ul>
 */
@Slf4j
@Component
public class KibOverdueEpisodeCalculator {

    /**
     * @return the episodes; empty if the client has no contracts or no overdue; null if the contracts have no
     * payment calendars to build them from - then the overdue stop factors use the legacy counters, and a period /
     * range without one sends the application to the underwriter
     */
    public List<OverdueEpisode> getOverdueEpisodes(CustomReport customReport, LocalDateTime requestDate) {
        List<Contract> contracts = clientContracts(customReport);
        if (contracts.isEmpty()) {
            return List.of();
        }
        long withoutCalendar = contracts.stream().filter(contract -> calendarItems(contract).isEmpty()).count();
        if (withoutCalendar == contracts.size()) {
            log.warn("[#getOverdueEpisodes] Нет платежных календарей КИБ по {} договорам, эпизоды просрочки не построены",
                    contracts.size());
            return null;
        }
        if (withoutCalendar > 0) {
            log.warn("[#getOverdueEpisodes] Нет платежного календаря КИБ у {} из {} договоров, их просрочки не учтены",
                    withoutCalendar, contracts.size());
        }

        LocalDate requestDay = requestDate.toLocalDate();
        List<OverdueEpisode> episodes = new ArrayList<>();
        for (Contract contract : contracts) {
            episodes.addAll(episodesOf(contract, requestDay));
        }
        log.debug("[#getOverdueEpisodes] Эпизодов просрочки: {}", episodes.size());
        return episodes;
    }

    private static List<OverdueEpisode> episodesOf(Contract contract, LocalDate requestDay) {
        List<CalendarItem> items = calendarItems(contract).stream()
                .filter(item -> item.date() != null && !item.date().toLocalDate().isAfter(requestDay))
                .sorted(Comparator.comparing(CalendarItem::date))
                .toList();

        List<OverdueEpisode> episodes = new ArrayList<>();
        CalendarItem previous = null;
        int maxDays = 0;
        LocalDate maxDay = null;
        for (CalendarItem item : items) {
            int days = pastDueDays(item);
            if (days > 0) {
                if (maxDay != null && !continuesEpisode(previous, item)) {
                    episodes.add(episode(maxDay, maxDays, requestDay));
                    maxDays = 0;
                    maxDay = null;
                }
                if (days >= maxDays) {
                    maxDays = days;
                    maxDay = item.date().toLocalDate();
                }
            } else if (maxDay != null) {
                episodes.add(episode(maxDay, maxDays, requestDay));
                maxDays = 0;
                maxDay = null;
            }
            previous = item;
        }
        // An overdue still open on the request date counts with the days reached by then
        if (maxDay != null) {
            episodes.add(episode(maxDay, maxDays, requestDay));
        }
        return episodes;
    }

    /**
     * Whether an overdue entry belongs to the episode of the overdue entry before it.
     * Now: consecutive overdue entries are one continuous overdue.
     * TODO confirm with a real KIB report: if pastDueDays is the overdue of each installment (paid in between),
     *  two entries are one episode only when the first overdue lasted past the next entry:
     *  {@code previous.date().plusDays(pastDueDays(previous)).isAfter(item.date())}
     */
    private static boolean continuesEpisode(CalendarItem previous, CalendarItem item) {
        return previous != null && pastDueDays(previous) > 0;
    }

    private static OverdueEpisode episode(LocalDate maxDay, int maxOverdueDays, LocalDate requestDay) {
        return new OverdueEpisode(monthsAgo(maxDay, requestDay), maxOverdueDays);
    }

    /** 1 - less than a month before the request, 2 - one to two months before it, and so on. */
    static int monthsAgo(LocalDate day, LocalDate requestDay) {
        return (int) MONTHS.between(day, requestDay) + 1;
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
