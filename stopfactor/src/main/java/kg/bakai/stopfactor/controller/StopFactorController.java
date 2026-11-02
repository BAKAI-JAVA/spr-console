package kg.bakai.stopfactor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.bakai.stopfactor.dto.scoring.*;
import kg.bakai.stopfactor.service.StopFactorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Scoring: stop factors", description = "Used by the scoring system")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stop-factors")
public class StopFactorController {

    private final StopFactorService stopFactorService;

    @Operation(summary = "Check an application against all active stop factors")
    @PostMapping("/check")
    public StopFactorCheckResponse check(@Valid @RequestBody StopFactorCheckRequest request) {
        return stopFactorService.check(request);
    }

    @Operation(summary = "Saved results of a scoring, with the stop factor versions they were made with")
    @GetMapping("/checks/{scoringId}")
    public List<StopFactorCheckDto> getChecks(@PathVariable long scoringId) {
        return stopFactorService.getChecks(scoringId);
    }

    @Operation(summary = "Active stop factors (read-only view, manage them through the admin API)")
    @GetMapping
    public List<StopFactorDto> getActiveStopFactors() {
        return stopFactorService.getActiveStopFactors();
    }

    @Operation(summary = "Conditions of the current version of a stop factor")
    @GetMapping("/{stopFactorId}/conditions")
    public List<StopFactorConditionDto> getConditions(@PathVariable long stopFactorId) {
        return stopFactorService.getConditions(stopFactorId);
    }
}
