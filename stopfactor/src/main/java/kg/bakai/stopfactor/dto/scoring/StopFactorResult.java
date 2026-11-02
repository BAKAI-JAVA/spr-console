package kg.bakai.stopfactor.dto.scoring;

import kg.bakai.stopfactor.enums.StopFactorStatus;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class StopFactorResult {
    private StopFactorStatus status;
    private String description;
    private String value;

    public StopFactorResult(String description, String value) {
        this.description = description;
        this.value = value;
        this.status = StopFactorStatus.APPROVED;
    }

    public StopFactorResult(StopFactorStatus status, String value) {
        this.description = null;
        this.value = value;
        this.status = status;
    }
}