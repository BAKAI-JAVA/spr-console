package kg.bakai.console.controller;

import kg.bakai.stopfactor.service.StopFactorConsoleService;
import kg.bakai.stopfactor.dto.ConditionDto;
import kg.bakai.stopfactor.dto.RuleDto;
import kg.bakai.stopfactor.dto.StopFactorResponse;
import kg.bakai.stopfactor.enums.StopFactorStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static kg.bakai.console.controller.ConsoleUser.HEADER;
import static kg.bakai.console.controller.ConsoleUser.orUnknown;

@Tag(name = "Console: rules and conditions", description = "Rules of the current version of a stop factor and their "
        + "conditions. Every change creates a new version and returns the stop factor: the rules get new ids, take them "
        + "from the response. changeReason is required.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stop-factors/{factorId}/rules")
public class StopFactorRuleConsoleController {

    private static final String CHANGE_REASON = "Why the change is made, kept in the version history";

    private final StopFactorConsoleService stopFactorConsoleService;

    @GetMapping
    public List<RuleDto> getRules(@PathVariable long factorId) {
        return stopFactorConsoleService.getRules(factorId);
    }

    @GetMapping("/{ruleId}")
    public RuleDto getRule(@PathVariable long factorId, @PathVariable long ruleId) {
        return stopFactorConsoleService.getRule(factorId, ruleId);
    }

    @Operation(summary = "Add a rule after the others",
            description = "A name left out is the metric's name template rendered with the params")
    @PostMapping
    public StopFactorResponse addRule(@PathVariable long factorId,
                                      @Valid @RequestBody RuleDto rule,
                                      @Parameter(description = CHANGE_REASON) @RequestParam String changeReason,
                                      @RequestHeader(name = HEADER, required = false) String user) {
        return stopFactorConsoleService.addRule(factorId, rule, changeReason, orUnknown(user));
    }

    @Operation(summary = "Replace a rule: name, params, conditions, active",
            description = "A name left out is kept when the params stay, otherwise rendered from the new params")
    @PutMapping("/{ruleId}")
    public StopFactorResponse updateRule(@PathVariable long factorId,
                                         @PathVariable long ruleId,
                                         @Valid @RequestBody RuleDto rule,
                                         @Parameter(description = CHANGE_REASON) @RequestParam String changeReason,
                                         @RequestHeader(name = HEADER, required = false) String user) {
        return stopFactorConsoleService.updateRule(factorId, ruleId, rule, changeReason, orUnknown(user));
    }

    @Operation(summary = "Remove a rule", description = "The only rule of a stop factor cannot be removed")
    @DeleteMapping("/{ruleId}")
    public StopFactorResponse deleteRule(@PathVariable long factorId,
                                         @PathVariable long ruleId,
                                         @Parameter(description = CHANGE_REASON) @RequestParam String changeReason,
                                         @RequestHeader(name = HEADER, required = false) String user) {
        return stopFactorConsoleService.deleteRule(factorId, ruleId, changeReason, orUnknown(user));
    }

    @GetMapping("/{ruleId}/conditions")
    public List<ConditionDto> getConditions(@PathVariable long factorId, @PathVariable long ruleId) {
        return stopFactorConsoleService.getConditions(factorId, ruleId);
    }

    @Operation(summary = "Set the condition of a result", description = "Adds the REJECTED or UNDERWRITER condition "
            + "of the body's result, or replaces it. Operators: GET /api/v1/admin/metrics/calculators, allowedOperators")
    @PutMapping("/{ruleId}/conditions")
    public StopFactorResponse saveCondition(@PathVariable long factorId,
                                            @PathVariable long ruleId,
                                            @Valid @RequestBody ConditionDto condition,
                                            @Parameter(description = CHANGE_REASON) @RequestParam String changeReason,
                                            @RequestHeader(name = HEADER, required = false) String user) {
        return stopFactorConsoleService.saveCondition(factorId, ruleId, condition, changeReason, orUnknown(user));
    }

    @Operation(summary = "Remove the condition of a result", description = "A rule without conditions is checked "
            + "and saved, but decides nothing")
    @DeleteMapping("/{ruleId}/conditions/{result}")
    public StopFactorResponse deleteCondition(@PathVariable long factorId,
                                              @PathVariable long ruleId,
                                              @PathVariable StopFactorStatus result,
                                              @Parameter(description = CHANGE_REASON) @RequestParam String changeReason,
                                              @RequestHeader(name = HEADER, required = false) String user) {
        return stopFactorConsoleService.deleteCondition(factorId, ruleId, result, changeReason, orUnknown(user));
    }
}
