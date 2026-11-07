package kg.bakai.spr.core.kib;

import kg.bakai.spr.core.kib.dto.OverdueForStopFactor;
import kg.bakai.spr.core.kib.dto.response.CustomReport;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * DUMMY of the SPR service with the same name and package. Only the method the stop factor check uses.
 * The overdue months of the 2026 rules are not read from here: KibOverdueHistoryReader finds them in the custom report.
 */
@Service
public class KibService {

    /**
     * Legacy fixed overdue counters. The real implementation counts them from the KIB custom report;
     * this one returns the counters the dummy report carries, zeros by default.
     */
    public OverdueForStopFactor getOverdueByPeriodAndCount(CustomReport customReport, LocalDateTime requestDate) {
        return customReport.overdue() != null ? customReport.overdue() : OverdueForStopFactor.NONE;
    }
}
