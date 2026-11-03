package kg.bakai.spr.core.kib.dto;

/**
 * DUMMY of the SPR class (individualReport.generalInfoDto()). Only what the stop factor check uses.
 */
public record GeneralInfoDto(
        int numberOfActiveDisputes,
        int numberOfUnjustifiedDisputesInPast,
        int creditInquiriesLast7Days
) {
}
