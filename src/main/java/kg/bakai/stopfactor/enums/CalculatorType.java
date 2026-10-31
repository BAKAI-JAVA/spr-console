package kg.bakai.stopfactor.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CalculatorType {
    OVERDUE_BUCKET_COUNT("Количество непрерывной просроченной задолженности"),
    RESTRUCTURING_COUNT("Количество реструктуризаций"),
    BUREAU_REQUESTS_COUNT("Количество запросов в кредитное бюро"),
    NEW_CREDITS_COUNT("Количество новых открытых кредитов"),
    KIB_REFUSAL_STATUSES("Отказные статусы в КИБ за всю историю клиента"),
    KIB_ACTIVE_DISPUTES("Количество активных споров КИБ"),
    KIB_UNJUSTIFIED_DISPUTES("Количество необоснованных споров в прошлом КИБ"),
    CURRENT_OVERDUE_DAYS("Текущее количество дней просрочки"),
    CURRENT_OVERDUE_AMOUNT("Текущая сумма просрочки"),
    OPEN_CREDITS_COUNT("Общее число открытых кредитов");

    private final String name;
}