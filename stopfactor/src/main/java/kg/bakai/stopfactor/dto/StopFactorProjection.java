package kg.bakai.stopfactor.dto;

import kg.bakai.stopfactor.enums.ConditionOperator;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;

public interface StopFactorProjection {
    long getStopFactorId();
    long getStopFactorVersionId();
    int getVersionNumber();
    String getStopFactorName();
    long getRuleId();
    /** Name of the rule; for a stop factor with one rule it is the stop factor name. */
    String getRuleName();
    StopFactorStatus getCheckResult();
    String getCheckedValue();
    String getRejectedValue();
    StopFactorValueTypes getRejectedValueType();
    ConditionOperator getRejectedOperator();
    String getUnderwriterValue();
    StopFactorValueTypes getUnderwriterValueType();
    ConditionOperator getUnderwriterOperator();
    /** Upper bound of the UNDERWRITER RANGE (the REJECTED value), null for GT and IN. */
    String getUnderwriterValueTo();
}