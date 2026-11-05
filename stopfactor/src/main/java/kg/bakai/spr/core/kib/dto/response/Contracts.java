package kg.bakai.spr.core.kib.dto.response;

import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DUMMY of the SPR class with the same name and package (contracts of the KIB custom report).
 * Only what KibOverdueEpisodeCalculator reads: the role of the client and the payment calendar of each contract.
 */
public record Contracts(ContractList contractList) {

    public record ContractList(List<Contract> contracts) {
    }

    public record Contract(RoleOfClient roleOfClient, PaymentCalendarList paymentCalendarList) {

        public record PaymentCalendarList(List<CalendarItem> paymentCalendarItems) {
        }

        /**
         * @param date        date of the calendar entry
         * @param pastDueDays days of overdue at this entry, 0 or null - no overdue
         */
        public record CalendarItem(LocalDateTime date, Integer pastDueDays) {
        }
    }
}
