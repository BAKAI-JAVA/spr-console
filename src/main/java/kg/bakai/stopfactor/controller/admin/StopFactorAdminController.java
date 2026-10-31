package kg.bakai.stopfactor.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.bakai.stopfactor.dto.admin.StopFactorRequest;
import kg.bakai.stopfactor.dto.admin.StopFactorResponse;
import kg.bakai.stopfactor.service.admin.StopFactorAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin: stop factors", description = "Every change creates a new version; checks keep the version they were made with")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stop-factors")
public class StopFactorAdminController {

    private static final String USER_HEADER = "X-User";
    private static final String UNKNOWN_USER = "unknown";

    private final StopFactorAdminService stopFactorAdminService;

    @Operation(summary = "Current versions of stop factors")
    @GetMapping
    public List<StopFactorResponse> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long metricId,
            @Parameter(description = "Stop factors that apply to this product, including the ones without product limits")
            @RequestParam(required = false) Long productId) {
        return stopFactorAdminService.getAll(active, metricId, productId);
    }

    @GetMapping("/{factorId}")
    public StopFactorResponse get(@PathVariable long factorId) {
        return stopFactorAdminService.get(factorId);
    }

    @Operation(summary = "All versions of a stop factor, newest first")
    @GetMapping("/{factorId}/versions")
    public List<StopFactorResponse> getVersions(@PathVariable long factorId) {
        return stopFactorAdminService.getVersions(factorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StopFactorResponse create(@Valid @RequestBody StopFactorRequest request,
                                     @RequestHeader(name = USER_HEADER, required = false) String user) {
        return stopFactorAdminService.create(request, userOrUnknown(user));
    }

    @Operation(summary = "Replace a stop factor", description = "Creates a new version and closes the current one. "
            + "Returns the current version unchanged when nothing differs. changeReason is required.")
    @PutMapping("/{factorId}")
    public StopFactorResponse update(@PathVariable long factorId,
                                     @Valid @RequestBody StopFactorRequest request,
                                     @RequestHeader(name = USER_HEADER, required = false) String user) {
        return stopFactorAdminService.update(factorId, request, userOrUnknown(user));
    }

    @Operation(summary = "Activate or deactivate a stop factor", description = "Creates a new version with the same definition")
    @PatchMapping("/{factorId}/status")
    public StopFactorResponse changeStatus(@PathVariable long factorId,
                                           @RequestParam boolean active,
                                           @Parameter(description = "Why the status is changed, kept in the version history")
                                           @RequestParam String changeReason,
                                           @RequestHeader(name = USER_HEADER, required = false) String user) {
        return stopFactorAdminService.changeStatus(factorId, active, changeReason, userOrUnknown(user));
    }

    @Operation(summary = "Change display order", description = "Body: every current stop factor id (active and inactive) "
            + "in the new order, e.g. [1, 2, 27, 3]. Updated in place without new versions, the order does not affect decisions.")
    @PutMapping("/order")
    public List<StopFactorResponse> reorder(@RequestBody List<Long> factorIds) {
        return stopFactorAdminService.reorder(factorIds);
    }

    private static String userOrUnknown(String user) {
        return user == null || user.isBlank() ? UNKNOWN_USER : user.trim();
    }
}
