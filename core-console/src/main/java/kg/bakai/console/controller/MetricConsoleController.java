package kg.bakai.console.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.bakai.stopfactor.dto.console.CalculatorDto;
import kg.bakai.stopfactor.dto.console.MetricDto;
import kg.bakai.stopfactor.service.console.MetricConsoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Console: metrics", description = "What a stop factor measures and which params it takes")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/metrics")
public class MetricConsoleController {

    private final MetricConsoleService metricConsoleService;

    @Operation(summary = "Calculators a metric can be built on, with their params and allowed condition value types")
    @GetMapping("/calculators")
    public List<CalculatorDto> getCalculators() {
        return metricConsoleService.getCalculators();
    }

    @GetMapping
    public List<MetricDto> getAll() {
        return metricConsoleService.getAll();
    }

    @GetMapping("/{id}")
    public MetricDto get(@PathVariable long id) {
        return metricConsoleService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetricDto create(@Valid @RequestBody MetricDto request) {
        return metricConsoleService.create(request);
    }

    @Operation(summary = "Update a metric", description = "The calculator cannot be changed. Rejected when the new params "
            + "would make a current stop factor of this metric invalid. Existing stop factor names are not re-rendered.")
    @PutMapping("/{id}")
    public MetricDto update(@PathVariable long id, @Valid @RequestBody MetricDto request) {
        return metricConsoleService.update(id, request);
    }

    @Operation(summary = "Delete a metric", description = "Only possible while no stop factor version refers to it, otherwise deactivate it")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        metricConsoleService.delete(id);
    }
}
