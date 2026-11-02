package kg.bakai.stopfactor.service;

import kg.bakai.stopfactor.dto.scoring.StopFactorCheckDto;
import kg.bakai.stopfactor.dto.scoring.StopFactorCheckRequest;
import kg.bakai.stopfactor.dto.scoring.StopFactorCheckResponse;
import kg.bakai.stopfactor.dto.scoring.StopFactorConditionDto;
import kg.bakai.stopfactor.dto.scoring.StopFactorDto;

import java.util.List;

public interface StopFactorService {

    StopFactorCheckResponse check(StopFactorCheckRequest request);

    List<StopFactorDto> getActiveStopFactors();

    List<StopFactorConditionDto> getConditions(long stopFactorId);

    List<StopFactorCheckDto> getChecks(long scoringId);

    /**
     * @return true if any stop factor version or saved check refers to the product
     */
    boolean isProductUsed(long productId);
}
