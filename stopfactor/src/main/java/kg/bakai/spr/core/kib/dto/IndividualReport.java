package kg.bakai.spr.core.kib.dto;

import java.util.List;

/**
 * DUMMY of the SPR class with the same name and package (KIB individual report). Only what the stop factor check uses.
 */
public record IndividualReport(
        GeneralInfoDto generalInfoDto,
        List<LoanDto> loanDtoList
) {
}
