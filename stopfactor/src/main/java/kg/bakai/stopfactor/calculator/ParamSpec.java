package kg.bakai.stopfactor.calculator;

import kg.bakai.stopfactor.entity.MetricParamSchema;

import java.util.Set;

public record ParamSpec(String key, String label, boolean required, int min, Set<String> allowedValues) {

    public static ParamSpec integer(String key, String label, boolean required, int min) {
        return new ParamSpec(key, label, required, min, Set.of());
    }

    public static ParamSpec oneOf(String key, String label, Set<String> allowedValues) {
        return new ParamSpec(key, label, true, 0, allowedValues);
    }

    public boolean isInteger() {
        return allowedValues.isEmpty();
    }

    /** The param as a metric stores it, for a metric created without params. */
    public MetricParamSchema toSchema() {
        return new MetricParamSchema(key, label, required);
    }
}
