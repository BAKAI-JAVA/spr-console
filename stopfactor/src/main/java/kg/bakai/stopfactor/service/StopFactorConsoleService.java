package kg.bakai.stopfactor.service;

import kg.bakai.common.enums.ProductSegment;
import kg.bakai.stopfactor.dto.ConditionDto;
import kg.bakai.stopfactor.dto.RuleDto;
import kg.bakai.stopfactor.dto.StopFactorRequest;
import kg.bakai.stopfactor.dto.StopFactorResponse;
import kg.bakai.stopfactor.enums.StopFactorStatus;

import java.util.List;

public interface StopFactorConsoleService {

    /**
     * @param productId stop factors that check applications of this product (its segment and product limits)
     * @param segment   stop factors of this segment, including the ones without a segment
     */
    List<StopFactorResponse> getAll(Boolean active, Long metricId, Long productId, ProductSegment segment);

    StopFactorResponse get(long factorId);

    List<StopFactorResponse> getVersions(long factorId);

    StopFactorResponse create(StopFactorRequest request, String user);

    StopFactorResponse update(long factorId, StopFactorRequest request, String user);

    StopFactorResponse changeStatus(long factorId, boolean active, String changeReason, String user);

    List<StopFactorResponse> reorder(List<Long> factorIds);

    /**
     * Removes a stop factor from the console and from checks: a last, inactive version is created with the reason
     * and closed at once. The versions stay, so checks made with them still show their rules.
     */
    void delete(long factorId, String changeReason, String user);

    // Rules and conditions of the current version. Every change creates a new version, so the rules get new ids:
    // take them from the returned stop factor.

    List<RuleDto> getRules(long factorId);

    RuleDto getRule(long factorId, long ruleId);

    /** Adds a rule after the others. */
    StopFactorResponse addRule(long factorId, RuleDto rule, String changeReason, String user);

    /** Replaces a rule; a name left out is the metric's name template rendered with the params. */
    StopFactorResponse updateRule(long factorId, long ruleId, RuleDto rule, String changeReason, String user);

    StopFactorResponse deleteRule(long factorId, long ruleId, String changeReason, String user);

    List<ConditionDto> getConditions(long factorId, long ruleId);

    /** Adds the condition of its result (REJECTED or UNDERWRITER) to a rule, or replaces it. */
    StopFactorResponse saveCondition(long factorId, long ruleId, ConditionDto condition, String changeReason, String user);

    StopFactorResponse deleteCondition(long factorId, long ruleId, StopFactorStatus result, String changeReason, String user);
}
