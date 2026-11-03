package kg.bakai.spr.core.kib;

import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.stopfactor.dto.scoring.OverdueForStopFactor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * DUMMY of the SPR service with the same name and package. Only the method the stop factor check uses.
 * The real implementation counts overdue episodes of the KIB custom report; this one returns zeros.
 */
@Service
public class KibService {

    public OverdueForStopFactor getOverdueByPeriodAndCount(CustomReport customReport, LocalDateTime requestDate) {
        return new OverdueForStopFactor(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
