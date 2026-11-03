package kg.bakai.spr.core.ob.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DUMMY of the SPR class with the same name and package (OB credit). Only the getters the stop factor check uses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TotalDebtReportData {
    private long currentOverdueDaysCount;
    private BigDecimal currentOverdueSum;
    private LocalDate lastChangeDate;
}
