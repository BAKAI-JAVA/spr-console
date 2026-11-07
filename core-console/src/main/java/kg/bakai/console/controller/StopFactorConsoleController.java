package kg.bakai.console.controller;

import kg.bakai.common.enums.ProductSegment;
import kg.bakai.stopfactor.service.StopFactorConsoleService;
import kg.bakai.stopfactor.dto.StopFactorRequest;
import kg.bakai.stopfactor.dto.StopFactorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static kg.bakai.console.controller.ConsoleUser.HEADER;
import static kg.bakai.console.controller.ConsoleUser.orUnknown;

@Tag(name = "Console: stop factors", description = "Every change creates a new version; checks keep the version they were made with")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stop-factors")
public class StopFactorConsoleController {

    private final StopFactorConsoleService stopFactorConsoleService;

    @Operation(summary = "Current versions of stop factors")
    @GetMapping
    public List<StopFactorResponse> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long metricId,
            @Parameter(description = "Stop factors that check applications of this product: of its segment or without one, "
                    + "limited to it or without product limits")
            @RequestParam(required = false) Long productId,
            @Parameter(description = "Stop factors of this segment, including the ones without a segment")
            @RequestParam(required = false) ProductSegment segment) {
        return stopFactorConsoleService.getAll(active, metricId, productId, segment);
    }

    @GetMapping("/{factorId}")
    public StopFactorResponse get(@PathVariable long factorId) {
        return stopFactorConsoleService.get(factorId);
    }

    @Operation(summary = "All versions of a stop factor, newest first")
    @GetMapping("/{factorId}/versions")
    public List<StopFactorResponse> getVersions(@PathVariable long factorId) {
        return stopFactorConsoleService.getVersions(factorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StopFactorResponse create(@Valid @RequestBody StopFactorRequest request,
                                     @RequestHeader(name = HEADER, required = false) String user) {
        return stopFactorConsoleService.create(request, orUnknown(user));
    }

    @Operation(summary = "Replace a stop factor", description = "Creates a new version and closes the current one. "
            + "Returns the current version unchanged when nothing differs. changeReason is required.")
    @PutMapping("/{factorId}")
    public StopFactorResponse update(@PathVariable long factorId,
                                     @Valid @RequestBody StopFactorRequest request,
                                     @RequestHeader(name = HEADER, required = false) String user) {
        return stopFactorConsoleService.update(factorId, request, orUnknown(user));
    }

    @Operation(summary = "Activate or deactivate a stop factor", description = "Creates a new version with the same definition")
    @PatchMapping("/{factorId}/status")
    public StopFactorResponse changeStatus(@PathVariable long factorId,
                                           @RequestParam boolean active,
                                           @Parameter(description = "Why the status is changed, kept in the version history")
                                           @RequestParam String changeReason,
                                           @RequestHeader(name = HEADER, required = false) String user) {
        return stopFactorConsoleService.changeStatus(factorId, active, changeReason, orUnknown(user));
    }

    @Operation(summary = "Change display order", description = "Body: every current stop factor id (active and inactive) "
            + "in the new order, e.g. [1, 2, 27, 3]. Updated in place without new versions, the order does not affect decisions.")
    @PutMapping("/order")
    public List<StopFactorResponse> reorder(@RequestBody List<Long> factorIds) {
        return stopFactorConsoleService.reorder(factorIds);
    }

    @Operation(summary = "Delete a stop factor", description = "Removes it from the console and from checks. Its versions "
            + "are kept (a last inactive version records the reason), so earlier checks still show their rules.")
    @DeleteMapping("/{factorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long factorId,
                       @Parameter(description = "Why the stop factor is deleted, kept in the version history")
                       @RequestParam String changeReason,
                       @RequestHeader(name = HEADER, required = false) String user) {
        stopFactorConsoleService.delete(factorId, changeReason, orUnknown(user));
    }
}
