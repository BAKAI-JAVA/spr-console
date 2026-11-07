package kg.bakai.spr.core.shared;

import lombok.Getter;

import static kg.bakai.spr.core.shared.ScoringStatus.*;

@Getter
public enum InternalStatusCode {
    /// PENDING
    _100(PENDING, "В обработке", 100),

    /// APPROVED
    _200(APPROVED, "Одобрена", 200),
    _201(APPROVED, "Одобрена с дополнительным условием: необходимо согласие супруга/супруги", 201),

    /// ALTERNATIVE
    _250(ALTERNATIVE, "Предложена альтернатива", 250),
    _251(ALTERNATIVE, "По сроку", 251),
    _252(ALTERNATIVE, "По сумме", 252),
    _253(ALTERNATIVE, "По сумме и сроку", 253),
    _254(ALTERNATIVE, "По совокупной сумме в Банке", 254),
    _255(ALTERNATIVE, "Предложена альтернатива с дополнительным условием: необходимо согласие супруга/супруги", 255),

    /// UNDERWRITER
    _300(UNDERWRITER, "Андеррайтер по скор баллу", 300),
    _301(UNDERWRITER, "Андеррайтер по стоп-факторам", 301),
    _302(UNDERWRITER, "Андеррайтер с дополнительным условием: необходимо согласие супруга/супруги", 302),

    /// REJECTED
    // Общие
    _400(REJECTED, "Скоринг балл высокий", 400),
    _401(REJECTED, "Сотрудник компании не может быть рассмотрен андеррайтером", 401),

    // Социальный фонд
    _420(REJECTED, "Категория пенсии не подходит для выдачи кредита", 420),

    // КИБ
    _430(REJECTED, "Недостаточно данных в ответе КИБ", 430),
    _431(REJECTED, "Просрочка свыше 90 дней за последние 12 мес", 431),

    // ЧС
    _440(REJECTED, "Данный клиент в черном списке", 440),
    _441(REJECTED, "Супруг(-а) данного клиента в черном списке", 441),
    _442(REJECTED, "Клиент добавлен в ЧС", 442),

    // Стоп-фактор
    _450(REJECTED, "Отказ по стоп-факторам", 450),
    _451(REJECTED, "Текущая просрочка по дням и\\или по сумме", 451),

    // Транзакционный анализ
    _460(REJECTED, "Отказ по транзакционному анализу", 460),

    // Offer
    _465(REJECTED, "Отказ в связи с недостаточностью дохода", 465),
    _466(REJECTED, "Отказ в связи с наличием совокупной задолженности", 466),
    _467(REJECTED, "Альтернативная сумма меньше минимальной суммы по продукту", 467),

    // Расчет платежеспособности
    _470(REJECTED, "Отказ по расчету платежеспособности", 470),
    _471(REJECTED, "Отсутствие доходов и кредитной истории за все годы", 471),
    _472(REJECTED, "Клиент АГРО АБС ОБ", 472),
    _473(REJECTED, "По анкетным данным безработный", 473),
    _474(REJECTED, "Наемный сотрудник, не прошел вариацию по СФ", 474),
    _475(REJECTED, "Наличие супруга/супруги или отсутствие данных в Түндүк", 475),

    /// ERROR
    // Общие
    _500(ERROR, "Внутренняя ошибка", 500),

    // Социальный фонд
    _520(ERROR, "Ошибка в СФ", 520),
    _521(ERROR, "Ошибка при обработке данных c Соц. фонда", 521),
    _522(ERROR, "Ошибка при получении данных с Соц. фонда", 522),

    // КИБ
    _530(ERROR, "Ошибка в КИБ", 530),
    _531(ERROR, "Ошибка при обработке данных c КИБ", 531),
    _532(ERROR, "Ошибка при получении данных с КИБ", 532),

    // ЧС
    _540(ERROR, "Ошибка в ЧС СПР", 540),

    _565(ERROR, "Ошибка в OfferService", 565),

    _UNKNOWN(null, null, 0);

    private final ScoringStatus status;
    private final String description;
    private final int code;

    InternalStatusCode(ScoringStatus status, String description, int code) {
        this.status = status;
        this.description = description;
        this.code = code;
    }

    public static InternalStatusCode fromCode(int code) {
        for (InternalStatusCode val : values()) {
            if (val.code == code) {
                return val;
            }
        }
        throw new IllegalArgumentException("Неизвестный код InternalStatusCode: " + code);
    }
}