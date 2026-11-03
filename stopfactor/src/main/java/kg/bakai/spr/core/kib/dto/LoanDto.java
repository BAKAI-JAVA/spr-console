package kg.bakai.spr.core.kib.dto;

import kg.bakai.spr.core.kib.dto.enums.LoanStatus;
import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DUMMY of the SPR class with the same name and package. Only the getters the stop factor check uses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanDto {
    private RoleOfClient participationType;
    private LoanStatus loanStatus;
    private int numberOfRestructuringsLast12Months;
    private int numberOfRestructuringsFrom12To24Months;
    private int daysDelay;
}
