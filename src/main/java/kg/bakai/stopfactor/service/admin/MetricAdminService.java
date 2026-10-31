package kg.bakai.stopfactor.service.admin;

import kg.bakai.stopfactor.dto.admin.CalculatorDto;
import kg.bakai.stopfactor.dto.admin.MetricDto;

import java.util.List;

public interface MetricAdminService {

    List<CalculatorDto> getCalculators();

    List<MetricDto> getAll();

    MetricDto get(long id);

    MetricDto create(MetricDto request);

    MetricDto update(long id, MetricDto request);

    void delete(long id);
}
