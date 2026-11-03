package kg.bakai.spr.core.ob.dto.response;

import kg.bakai.spr.core.ob.dto.TotalDebtReportData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * DUMMY of the SPR class with the same name and package (OB total debt). Only what the stop factor check uses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TotalDebtFinalResponse {
    private List<TotalDebtReportData> credits = new ArrayList<>();
}
