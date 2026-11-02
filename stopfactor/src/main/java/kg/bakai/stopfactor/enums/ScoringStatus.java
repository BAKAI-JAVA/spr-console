package kg.bakai.stopfactor.enums;

import lombok.Getter;

@Getter
public enum ScoringStatus {
    PENDING("В процессе", 0),
    APPROVED("Одобрен", 1),
    ALTERNATIVE("Предложена другая сумма или срок", 2),
    UNDERWRITER("Андеррайтер", 3),
    REJECTED("Отказ", 4),
    ERROR("Обработан с ошибкой", 5);

    private final String description;
    private final int weight;

    ScoringStatus(String description, int weight) {
        this.description = description;
        this.weight = weight;
    }
}