package kg.bakai.stopfactor.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StopFactorCheckRequest(
        @NotNull Long scoringId,
        List<String> creditHistoryStatuses,
        @NotNull StopFactorIntegerValueDto values
) {
}
