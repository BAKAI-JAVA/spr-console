package kg.bakai.stopfactor.dto.admin;

import kg.bakai.stopfactor.enums.CalculatorType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record StopFactorResponse(
        long factorId,
        long versionId,
        int versionNumber,
        boolean current,
        String name,
        String description,
        boolean active,
        int orderNumber,
        long metricId,
        String metricTitle,
        CalculatorType calculator,
        Map<String, Object> params,
        Set<Long> productIds,
        List<ConditionDto> conditions,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        String changeReason,
        String createdBy,
        LocalDateTime createdDate
) {
}
