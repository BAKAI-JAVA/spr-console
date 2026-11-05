package kg.bakai.stopfactor.calculator;

import java.util.List;
import java.util.Map;

/** Base of the calculators whose stop factors have no params. */
public abstract class ParameterlessCalculator extends AbstractStopFactorCalculator<NoParams> {

    @Override
    public List<ParamSpec> paramSpecs() {
        return List.of();
    }

    @Override
    public NoParams parseParams(Map<String, Object> params) {
        return NoParams.INSTANCE;
    }
}
