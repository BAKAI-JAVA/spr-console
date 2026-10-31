package kg.bakai.stopfactor.repository.projection;

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
    String getUnderwriterValue();
    StopFactorValueTypes getUnderwriterValueType();
}