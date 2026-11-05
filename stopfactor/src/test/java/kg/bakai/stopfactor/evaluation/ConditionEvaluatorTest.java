package kg.bakai.stopfactor.evaluation;

import kg.bakai.stopfactor.calculator.CheckedValue;
import kg.bakai.stopfactor.entity.StopFactorCondition;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;
import kg.bakai.stopfactor.evaluation.ConditionEvaluator.Outcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kg.bakai.stopfactor.enums.StopFactorStatus.*;
import static kg.bakai.stopfactor.enums.StopFactorValueTypes.*;
import static org.assertj.core.api.Assertions.assertThat;

class ConditionEvaluatorTest {

    @Test
    void numberConditionFiresWhenTheValueIsGreater() {
        List<StopFactorCondition> conditions = List.of(condition(REJECTED, INTEGER, "8"), condition(UNDERWRITER, INTEGER, "5"));

        assertThat(apply(5, conditions)).isEqualTo(new Outcome(APPROVED, "5.0"));
        assertThat(apply(6, conditions)).isEqualTo(new Outcome(UNDERWRITER, "6.0"));
        assertThat(apply(8, conditions)).isEqualTo(new Outcome(UNDERWRITER, "8.0"));
        assertThat(apply(9, conditions)).isEqualTo(new Outcome(REJECTED, "9.0"));
    }

    @Test
    void doubleCondition() {
        assertThat(apply(1500.75, List.of(condition(REJECTED, DOUBLE, "1500.5"))).result()).isEqualTo(REJECTED);
        assertThat(apply(1500.5, List.of(condition(REJECTED, DOUBLE, "1500.5"))).result()).isEqualTo(APPROVED);
    }

    @Test
    void noConditionsApproveWithTheValue() {
        assertThat(apply(3, List.of())).isEqualTo(new Outcome(APPROVED, "3.0"));
    }

    @Test
    void statusConditionShowsTheFirstLoanStatusFound() {
        List<StopFactorCondition> conditions = List.of(
                condition(REJECTED, ARRAY, "WrittenOff, SoldToThirdParty"),
                condition(UNDERWRITER, ARRAY, "TransferredToOtherReportingInst"));

        assertThat(statuses(conditions, "Open", "SoldToThirdParty", "WrittenOff")).isEqualTo(new Outcome(REJECTED, "SoldToThirdParty"));
        assertThat(statuses(conditions, "TransferredToOtherReportingInst")).isEqualTo(new Outcome(UNDERWRITER, "TransferredToOtherReportingInst"));
        assertThat(statuses(conditions, "Open")).isEqualTo(new Outcome(APPROVED, ConditionEvaluator.NO_MATCHES));
    }

    @Test
    void statusConditionWithoutSpacesUnknownStatusesAndStringType() {
        assertThat(statuses(List.of(condition(REJECTED, ARRAY, "NoSuchStatus,WrittenOff")), "WrittenOff").result()).isEqualTo(REJECTED);
        assertThat(statuses(List.of(condition(REJECTED, ARRAY, "writtenoff")), "WrittenOff").result()).isEqualTo(APPROVED);
        assertThat(statuses(List.of(condition(REJECTED, STRING, "WrittenOff")), "WrittenOff").result()).isEqualTo(REJECTED);
    }

    private static Outcome apply(double value, List<StopFactorCondition> conditions) {
        return ConditionEvaluator.apply(CheckedValue.ofNumber(value), conditions);
    }

    private static Outcome statuses(List<StopFactorCondition> conditions, String... loanStatuses) {
        return ConditionEvaluator.apply(CheckedValue.ofValues(List.of(loanStatuses)), conditions);
    }

    private static StopFactorCondition condition(StopFactorStatus result, StopFactorValueTypes type, String value) {
        StopFactorCondition condition = new StopFactorCondition();
        condition.setResult(result);
        condition.setValueType(type);
        condition.setValue(value);
        return condition;
    }
}
