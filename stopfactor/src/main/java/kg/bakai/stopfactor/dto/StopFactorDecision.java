package kg.bakai.stopfactor.dto;

import kg.bakai.spr.core.shared.InternalStatusCode;
import kg.bakai.stopfactor.enums.StopFactorStatus;

public record StopFactorDecision(
        StopFactorStatus status,
        InternalStatusCode internalStatusCode
) {
}