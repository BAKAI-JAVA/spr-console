package kg.bakai.stopfactor.evaluation;

import kg.bakai.stopfactor.calculator.CheckedValue;
import kg.bakai.stopfactor.entity.StopFactorCondition;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kg.bakai.stopfactor.enums.StopFactorStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConditionMatcherTest {

    private final ConditionMatcher matcher = new ConditionMatcher();

    private static StopFactorCondition condition(StopFactorStatus result, StopFactorValueTypes type, String value) {
        StopFactorCondition condition = new StopFactorCondition();
        condition.setResult(result);
        condition.setValueType(type);
        condition.setValue(value);
        return condition;
    }

    @Test
    void numericThresholdTriggersOnlyAboveTheValue() {
        List<StopFactorCondition> conditions = List.of(
                condition(REJECTED, StopFactorValueTypes.INTEGER, "7"),
                condition(UNDERWRITER, StopFactorValueTypes.INTEGER, "5"));

        assertThat(matcher.evaluate(CheckedValue.ofNumber(5), conditions)).isEqualTo(APPROVED);
        assertThat(matcher.evaluate(CheckedValue.ofNumber(6), conditions)).isEqualTo(UNDERWRITER);
        assertThat(matcher.evaluate(CheckedValue.ofNumber(7), conditions)).isEqualTo(UNDERWRITER);
        assertThat(matcher.evaluate(CheckedValue.ofNumber(8), conditions)).isEqualTo(REJECTED);
    }

    @Test
    void anyOverdueAmountAboveZeroIsRejected() {
        List<StopFactorCondition> conditions = List.of(condition(REJECTED, StopFactorValueTypes.DOUBLE, "0"));

        assertThat(matcher.evaluate(CheckedValue.ofNumber(0), conditions)).isEqualTo(APPROVED);
        assertThat(matcher.evaluate(CheckedValue.ofNumber(0.01), conditions)).isEqualTo(REJECTED);
    }

    @Test
    void statusListsMatchCaseInsensitivelyAndRejectedWins() {
        List<StopFactorCondition> conditions = List.of(
                condition(REJECTED, StopFactorValueTypes.ARRAY, "WrittenOff, SoldToThirdParty"),
                condition(UNDERWRITER, StopFactorValueTypes.ARRAY, "TransferredToOtherReportingInst"));

        assertThat(matcher.evaluate(CheckedValue.ofValues(List.of("Open")), conditions)).isEqualTo(APPROVED);
        assertThat(matcher.evaluate(CheckedValue.ofValues(List.of(" transferredtootherreportinginst ")), conditions)).isEqualTo(UNDERWRITER);
        assertThat(matcher.evaluate(CheckedValue.ofValues(List.of("TransferredToOtherReportingInst", "writtenoff")), conditions)).isEqualTo(REJECTED);
    }

    @Test
    void conditionThatDoesNotFitTheValueFailsInsteadOfPassing() {
        List<StopFactorCondition> conditions = List.of(condition(REJECTED, StopFactorValueTypes.ARRAY, "WrittenOff"));

        assertThatThrownBy(() -> matcher.evaluate(CheckedValue.ofNumber(1), conditions))
                .isInstanceOf(IllegalStateException.class);
    }
}
