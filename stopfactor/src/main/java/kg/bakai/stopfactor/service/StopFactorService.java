package kg.bakai.stopfactor.service;

import kg.bakai.spr.core.kib.dto.ConsolidationDto;
import kg.bakai.spr.core.kib.dto.IndividualReport;
import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.spr.core.ob.dto.response.TotalDebtFinalResponse;
import kg.bakai.stopfactor.dto.scoring.StopFactorConditionDto;
import kg.bakai.stopfactor.dto.scoring.StopFactorDecision;
import kg.bakai.stopfactor.repository.projection.StopFactorProjection;

import java.time.LocalDate;
import java.util.List;

public interface StopFactorService {
    StopFactorDecision check(long scoringId, CustomReport customReport, IndividualReport individualReport, ConsolidationDto consolidationDto, TotalDebtFinalResponse totalDebt, LocalDate requestDate);

    void save(String name, String description);

    void saveCondition(StopFactorConditionDto dto);

    List<StopFactorProjection> findByScoringId(Long scoringId);
}
