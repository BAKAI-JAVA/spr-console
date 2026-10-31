package kg.bakai.stopfactor.service;

import kg.bakai.stopfactor.dto.StopFactorCheckDto;
import kg.bakai.stopfactor.dto.StopFactorCheckRequest;
import kg.bakai.stopfactor.dto.StopFactorCheckResponse;
import kg.bakai.stopfactor.dto.StopFactorConditionDto;
import kg.bakai.stopfactor.dto.StopFactorDto;

import java.util.List;

public interface StopFactorService {

    StopFactorCheckResponse check(StopFactorCheckRequest request);

    List<StopFactorDto> getActiveStopFactors();

    List<StopFactorConditionDto> getConditions(long stopFactorId);

    List<StopFactorCheckDto> getChecks(long scoringId);
}
