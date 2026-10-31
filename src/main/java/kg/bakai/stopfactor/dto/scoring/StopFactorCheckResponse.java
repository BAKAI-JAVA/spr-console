package kg.bakai.stopfactor.dto.scoring;

import java.util.List;

public record StopFactorCheckResponse(
        long scoringId,
        StopFactorDecision decision,
        List<StopFactorResult> results
) {
}
