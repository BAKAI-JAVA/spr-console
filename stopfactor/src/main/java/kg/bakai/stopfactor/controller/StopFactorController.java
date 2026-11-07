package kg.bakai.stopfactor.controller;

import kg.bakai.stopfactor.service.StopFactorService;
import kg.bakai.stopfactor.dto.StopFactorProjection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The check itself is not a REST endpoint: SPR calls {@link StopFactorService#check} directly with the KIB/OB reports.
 * Stop factors and their conditions are managed through the console API (/api/v1/admin/stop-factors).
 */
@Tag(name = "Scoring: stop factors", description = "Results of stop factor checks of SPR scorings")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stop-factors")
public class StopFactorController {

    private final StopFactorService stopFactorService;

    @Operation(summary = "Saved results of a scoring, with the stop factor versions they were made with")
    @GetMapping("/checks/{scoringId}")
    public List<StopFactorProjection> findByScoringId(@PathVariable long scoringId) {
        return stopFactorService.findByScoringId(scoringId);
    }
}
