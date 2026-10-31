package kg.bakai.stopfactor.service.admin;

import kg.bakai.stopfactor.dto.admin.StopFactorRequest;
import kg.bakai.stopfactor.dto.admin.StopFactorResponse;

import java.util.List;

public interface StopFactorAdminService {

    List<StopFactorResponse> getAll(Boolean active, Long metricId, Long productId);

    StopFactorResponse get(long factorId);

    List<StopFactorResponse> getVersions(long factorId);

    StopFactorResponse create(StopFactorRequest request, String user);

    StopFactorResponse update(long factorId, StopFactorRequest request, String user);

    StopFactorResponse changeStatus(long factorId, boolean active, String changeReason, String user);

    List<StopFactorResponse> reorder(List<Long> factorIds);
}
