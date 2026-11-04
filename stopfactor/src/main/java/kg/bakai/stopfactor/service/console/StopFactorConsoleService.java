package kg.bakai.stopfactor.service.console;

import kg.bakai.stopfactor.dto.console.StopFactorRequest;
import kg.bakai.stopfactor.dto.console.StopFactorResponse;
import kg.bakai.common.enums.ProductSegment;

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
}
