package kg.bakai.stopfactor.repository.projection;

import kg.bakai.stopfactor.enums.ConditionOperator;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;

public interface StopFactorProjection {
    long getStopFactorId();
    long getStopFactorVersionId();
    int getVersionNumber();
    String getStopFactorName();
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