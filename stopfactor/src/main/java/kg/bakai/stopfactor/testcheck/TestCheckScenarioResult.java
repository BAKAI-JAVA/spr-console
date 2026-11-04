package kg.bakai.stopfactor.testcheck;

import kg.bakai.stopfactor.enums.InternalStatusCode;
import kg.bakai.stopfactor.enums.StopFactorStatus;

public record TestCheckScenarioResult(
        String scenario,
        String description,
        StopFactorStatus expectedDecision,
        InternalStatusCode expectedInternalStatusCode,
        boolean asExpected,
        TestCheckResponse result
) {
}
