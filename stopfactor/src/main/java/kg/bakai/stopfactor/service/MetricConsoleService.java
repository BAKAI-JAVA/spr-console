package kg.bakai.stopfactor.service;

import kg.bakai.stopfactor.dto.CalculatorDto;
import kg.bakai.stopfactor.dto.MetricDto;

import java.util.List;

public interface MetricConsoleService {

    List<CalculatorDto> getCalculators();

    List<MetricDto> getAll();

    MetricDto get(long id);

    MetricDto create(MetricDto request);

    MetricDto update(long id, MetricDto request);

    void delete(long id);
}
