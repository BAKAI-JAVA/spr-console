package kg.bakai.stopfactor.controller;

import jakarta.validation.Valid;
import kg.bakai.stopfactor.dto.StopFactorCheckDto;
import kg.bakai.stopfactor.dto.StopFactorCheckRequest;
import kg.bakai.stopfactor.dto.StopFactorCheckResponse;
import kg.bakai.stopfactor.dto.StopFactorConditionDto;
import kg.bakai.stopfactor.dto.StopFactorDto;
import kg.bakai.stopfactor.service.StopFactorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stop-factors")
public class StopFactorController {

    private final StopFactorService stopFactorService;

    @GetMapping
    public List<StopFactorDto> getActiveStopFactors() {
        return stopFactorService.getActiveStopFactors();
    }

    @GetMapping("/{stopFactorId}/conditions")
    public List<StopFactorConditionDto> getConditions(@PathVariable long stopFactorId) {
        return stopFactorService.getConditions(stopFactorId);
    }

    @PostMapping("/check")
    public StopFactorCheckResponse check(@Valid @RequestBody StopFactorCheckRequest request) {
        return stopFactorService.check(request);
    }

    @GetMapping("/checks/{scoringId}")
    public List<StopFactorCheckDto> getChecks(@PathVariable long scoringId) {
        return stopFactorService.getChecks(scoringId);
    }
}
