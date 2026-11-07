package kg.bakai.spr.core.kib.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DUMMY of the SPR class with the same name and package: "Contracts" of the KIB custom report (CreditinfoReportPlus).
 * Only what KibOverdueHistoryReader reads, mapped from the KIB JSON names.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Contracts(@JsonProperty("ContractList") ContractList contractList) {

    /** Date-time format of the KIB report, e.g. "2025-04-30 00:00:00". */
    public static final String KIB_DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContractList(@JsonProperty("Contract") List<Contract> contracts) {
    }

    /**
     * @param roleOfClient        role of the client in the contract
     * @param startDate           start of the contract
     * @param paymentCalendarList monthly history of the contract
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contract(
            @JsonProperty("RoleOfClient") RoleOfClient roleOfClient,
            @JsonProperty("StartDate") @JsonFormat(pattern = KIB_DATE_TIME) LocalDateTime startDate,
            @JsonProperty("PaymentCalendarList") PaymentCalendarList paymentCalendarList
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record PaymentCalendarList(@JsonProperty("CalendarItem") List<CalendarItem> paymentCalendarItems) {
        }

        /**
         * One month of the payment calendar. KIB sends one entry per month, dated the last day of the month, for
         * the last 12 or 24 months - also months before the contract started (all zeros).
         *
         * @param date           last day of the month
         * @param pastDueDays    overdue days at the end of the month: above 0 - the overdue is not repaid and goes on
         *                       into the next month
         * @param maxPastDueDays largest overdue days reached during the month, also when repaid before its end
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record CalendarItem(
                @JsonProperty("Date") @JsonFormat(pattern = KIB_DATE_TIME) LocalDateTime date,
                @JsonProperty("PastDueDays") Integer pastDueDays,
                @JsonProperty("MaxPastDueDays") Integer maxPastDueDays
        ) {
        }
    }
}
