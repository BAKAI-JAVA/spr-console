package kg.bakai.stopfactor.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.bakai.stopfactor.dto.admin.CalculatorDto;
import kg.bakai.stopfactor.dto.admin.MetricDto;
import kg.bakai.stopfactor.service.admin.MetricAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin: metrics", description = "What a stop factor measures and which params it takes")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/metrics")
public class MetricAdminController {

    private final MetricAdminService metricAdminService;

    @Operation(summary = "Calculators a metric can be built on, with their params and allowed condition value types")
    @GetMapping("/calculators")
    public List<CalculatorDto> getCalculators() {
        return metricAdminService.getCalculators();
    }

    @GetMapping
    public List<MetricDto> getAll() {
        return metricAdminService.getAll();
    }

    @GetMapping("/{id}")
    public MetricDto get(@PathVariable long id) {
        return metricAdminService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetricDto create(@Valid @RequestBody MetricDto request) {
        return metricAdminService.create(request);
    }

    @Operation(summary = "Update a metric", description = "The calculator cannot be changed. Rejected when the new params "
            + "would make a current stop factor of this metric invalid. Existing stop factor names are not re-rendered.")
    @PutMapping("/{id}")
    public MetricDto update(@PathVariable long id, @Valid @RequestBody MetricDto request) {
        return metricAdminService.update(id, request);
    }

    @Operation(summary = "Delete a metric", description = "Only possible while no stop factor version refers to it, otherwise deactivate it")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        metricAdminService.delete(id);
    }
}
