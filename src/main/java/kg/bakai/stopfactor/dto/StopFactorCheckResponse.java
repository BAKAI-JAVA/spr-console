package kg.bakai.stopfactor.dto;

import java.util.List;

public record StopFactorCheckResponse(
        long scoringId,
        StopFactorDecision decision,
        List<StopFactorResult> results
) {
}
