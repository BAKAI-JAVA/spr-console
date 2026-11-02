package kg.bakai.stopfactor.dto.scoring;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StopFactorCheckRequest(
        @NotNull Long scoringId,
        Long productId,
        List<String> creditHistoryStatuses,
        @NotNull @Valid StopFactorIntegerValueDto values,
        @Valid CreditHistoryEvents history
) {
}
