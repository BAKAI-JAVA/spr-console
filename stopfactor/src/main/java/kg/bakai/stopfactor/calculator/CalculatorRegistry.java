package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.enums.CalculatorType;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CalculatorRegistry {

    private final Map<CalculatorType, StopFactorCalculator<?>> calculators = new EnumMap<>(CalculatorType.class);

    public CalculatorRegistry(List<StopFactorCalculator<?>> calculators) {
        for (StopFactorCalculator<?> calculator : calculators) {
            if (this.calculators.put(calculator.type(), calculator) != null) {
                throw new IllegalStateException("Для " + calculator.type() + " зарегистрировано больше одного калькулятора");
            }
        }
        EnumSet<CalculatorType> missing = EnumSet.allOf(CalculatorType.class);
        missing.removeAll(this.calculators.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Нет калькулятора для " + missing);
        }
    }

    public StopFactorCalculator<?> get(CalculatorType type) {
        return calculators.get(type);
    }

    public Collection<StopFactorCalculator<?>> all() {
        return Collections.unmodifiableCollection(calculators.values());
    }
}
