package kg.bakai.stopfactor.service.console;

import kg.bakai.stopfactor.dto.console.CalculatorDto;
import kg.bakai.stopfactor.dto.console.MetricDto;

import java.util.List;

public interface MetricConsoleService {

    List<CalculatorDto> getCalculators();

    List<MetricDto> getAll();

    MetricDto get(long id);

    MetricDto create(MetricDto request);

    MetricDto update(long id, MetricDto request);

    void delete(long id);
}
