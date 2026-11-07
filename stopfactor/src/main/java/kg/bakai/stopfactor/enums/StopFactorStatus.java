package kg.bakai.stopfactor.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** The decision of a stop factor; the label is what the console and messages call it. */
@Getter
@RequiredArgsConstructor
public enum StopFactorStatus {
    APPROVED("Одобрено"),
    REJECTED("Отказ"),
    UNDERWRITER("Андеррайтер");

    private final String label;
}
