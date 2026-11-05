package kg.bakai.stopfactor.testcheck;

import kg.bakai.spr.core.kib.dto.enums.LoanStatus;
import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;
import kg.bakai.stopfactor.dto.scoring.CreditHistoryEvents.OverdueEpisode;
import kg.bakai.stopfactor.testcheck.TestCheckRequest.CalendarEntry;
import kg.bakai.stopfactor.testcheck.TestCheckRequest.Contract;
import kg.bakai.stopfactor.testcheck.TestCheckRequest.Kib;
import kg.bakai.stopfactor.testcheck.TestCheckRequest.KibPart;
import kg.bakai.stopfactor.testcheck.TestCheckRequest.Loan;
import kg.bakai.stopfactor.testcheck.TestCheckRequest.ObCredit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static kg.bakai.stopfactor.enums.InternalStatusCode._451;
import static kg.bakai.stopfactor.enums.StopFactorStatus.*;

/**
 * Test applications for the local database. Scorings 1..5 and their products come from changelog 1.2:
 * 1 Потребительский кредит, 2 Автокредит, 3 Ипотека, 4 Микрокредит (RETAIL), 5 Бизнес-кредит (MSB, changelog 1.5).
 */
final class TestCheckScenarios {

    private static final long CONSUMER = 1, CAR = 2, MORTGAGE = 3, MICRO = 4, BUSINESS = 5;
    private static final LocalDate DATE = LocalDate.of(2026, 11, 4);

    private TestCheckScenarios() {
    }

    static final List<TestCheckScenario> ALL = List.of(
            new TestCheckScenario("full-application",
                    "Все поля заполнены: Потребительский кредит, два кредита в КИБ, две старые просрочки в пределах лимитов, кредит в ОБ",
                    APPROVED, null, new TestCheckRequest(-100L, CONSUMER, DATE,
                    new Kib(0, 0, 1, BigDecimal.ZERO, 1, 2,
                            List.of(new Loan(RoleOfClient.MainDebtor, LoanStatus.Open, 0, 0, 0),
                                    new Loan(RoleOfClient.Guarantor, LoanStatus.FullyPaid, 0, 0, 0)),
                            List.of(new OverdueEpisode(2, 5), new OverdueEpisode(14, 12)),
                            null, false, null, Set.of()),
                    List.of(new ObCredit(0, BigDecimal.ZERO, LocalDate.of(2023, 5, 10))))),
            new TestCheckScenario("clean-retail",
                    "Чистая кредитная история, Розница",
                    APPROVED, null, request(CONSUMER, kib())),
            new TestCheckScenario("retail-overdue-reject",
                    "6 эпизодов по 5 дней в 1-3 месяце: у Розницы отказ с 6",
                    REJECTED, null, request(CONSUMER, kib().episodes(Collections.nCopies(6, new OverdueEpisode(2, 5))))),
            new TestCheckScenario("msb-same-history-yellow",
                    "Та же история у продукта МСБ: 6..10 эпизодов - жёлтая зона",
                    UNDERWRITER, null, request(BUSINESS, kib().episodes(Collections.nCopies(6, new OverdueEpisode(2, 5))))),
            new TestCheckScenario("retail-old-overdue-no-limit",
                    "Просрочки 1-5 дней в 7-12 месяце и до 30 дней в 25-36 месяце: без ограничений",
                    APPROVED, null, request(CONSUMER, kib().episodes(List.of(new OverdueEpisode(8, 5), new OverdueEpisode(30, 30))))),
            new TestCheckScenario("kib-payment-calendars",
                    "Платежные календари КИБ: просрочка 12 дней месяц назад (Ок), 25 дней 5 месяцев назад у поручителя "
                            + "(с 4 по 6 месяц от 21 дня - отказ), просрочка созаемщика не учитывается",
                    REJECTED, null, request(CONSUMER, kib().contracts(List.of(
                    new Contract(RoleOfClient.MainDebtor, List.of(
                            new CalendarEntry(LocalDate.of(2026, 9, 10), 4),
                            new CalendarEntry(LocalDate.of(2026, 10, 10), 12),
                            new CalendarEntry(LocalDate.of(2026, 11, 1), 0))),
                    new Contract(RoleOfClient.Guarantor, List.of(
                            new CalendarEntry(LocalDate.of(2026, 6, 15), 25),
                            new CalendarEntry(LocalDate.of(2026, 7, 15), 0))),
                    new Contract(RoleOfClient.CoDebtor, List.of(
                            new CalendarEntry(LocalDate.of(2026, 10, 1), 40))))))),
            new TestCheckScenario("product-limited-stop-factor",
                    "3 открытых кредита по Ипотеке: тестовый стоп-фактор только для Ипотеки и Автокредита (UNDERWRITER > 2)",
                    UNDERWRITER, null, request(MORTGAGE, kib().openCredits(3))),
            new TestCheckScenario("same-credits-other-product",
                    "3 открытых кредита по Потребительскому: тестовый стоп-фактор его не проверяет, общий #12 отказывает только с 8",
                    APPROVED, null, request(CONSUMER, kib().openCredits(3))),
            new TestCheckScenario("microcredit-bureau-requests",
                    "3 запроса в бюро за 7 дней по Микрокредиту: тестовый стоп-фактор UNDERWRITER > 2",
                    UNDERWRITER, null, request(MICRO, kib().bureauRequests(3))),
            new TestCheckScenario("new-credits-limit",
                    "4 новых кредита за 30 дней: #11 отказывает с 4",
                    REJECTED, null, request(CONSUMER, kib().newCredits(4))),
            new TestCheckScenario("current-overdue-451",
                    "Текущая просрочка 5 дней в ОБ: отказ с кодом 451",
                    REJECTED, _451, new TestCheckRequest(CONSUMER, null, DATE, kib().build(),
                    List.of(new ObCredit(5, new BigDecimal("1200.50"), DATE.minusMonths(1))))),
            new TestCheckScenario("refusal-status",
                    "Кредит клиента списан (WrittenOff): отказ по статусам КИБ",
                    REJECTED, null, request(CONSUMER, kib().loan(new Loan(RoleOfClient.MainDebtor, LoanStatus.WrittenOff, 0, 0, 0)))),
            new TestCheckScenario("refusal-status-of-co-debtor",
                    "Списан кредит, где клиент созаемщик: не учитывается",
                    APPROVED, null, request(CONSUMER, kib().loan(new Loan(RoleOfClient.CoDebtor, LoanStatus.WrittenOff, 0, 0, 0)))),
            new TestCheckScenario("transfer-status-underwriter",
                    "Кредит передан в другую организацию: андеррайтер",
                    UNDERWRITER, null, request(CONSUMER, kib().loan(new Loan(RoleOfClient.MainDebtor, LoanStatus.TransferredToOtherReportingInst, 0, 0, 0)))),

            // Missing data
            new TestCheckScenario("missing-kib-report",
                    "Нет отчёта КИБ: проверяются только данные ОБ, история просрочек считается пустой",
                    APPROVED, null, new TestCheckRequest(CAR, null, DATE, null,
                    List.of(new ObCredit(0, null, DATE.minusMonths(2))))),
            new TestCheckScenario("partial-kib-report",
                    "Отчёт КИБ без консолидации: 2 активных спора не проверяются, сервис считает, что отчёта КИБ нет",
                    APPROVED, null, request(CONSUMER, kib().activeDisputes(2).missing(KibPart.CONSOLIDATION))),
            new TestCheckScenario("full-kib-report-same-disputes",
                    "Тот же клиент с полным отчётом КИБ: 2 активных спора - отказ",
                    REJECTED, null, request(CONSUMER, kib().activeDisputes(2))),
            new TestCheckScenario("overdue-episodes-unavailable",
                    "В отчёте КИБ нет платежных календарей: эпизоды не построены, новые диапазоны дают NO_DATA, заявка уходит андеррайтеру",
                    UNDERWRITER, null, request(CONSUMER, kib().episodesUnavailable())),
            new TestCheckScenario("scoring-without-product",
                    "Скоринг без продукта: сегмент RETAIL, ограничения по продуктам не действуют",
                    APPROVED, null, new TestCheckRequest(null, null, DATE, kib().build(), List.of()))
    );

    private static TestCheckRequest request(long scoringId, KibBuilder kib) {
        return new TestCheckRequest(scoringId, null, DATE, kib.build(), List.of());
    }

    private static KibBuilder kib() {
        return new KibBuilder();
    }

    private static final class KibBuilder {
        private int activeDisputes;
        private int bureauRequests;
        private int newCredits;
        private int openCredits;
        private List<Loan> loans = List.of(new Loan(RoleOfClient.MainDebtor, LoanStatus.Open, 0, 0, 0));
        private List<OverdueEpisode> episodes = List.of();
        private List<Contract> contracts;
        private boolean episodesUnavailable;
        private Set<KibPart> missing = Set.of();

        KibBuilder activeDisputes(int count) { activeDisputes = count; return this; }
        KibBuilder bureauRequests(int count) { bureauRequests = count; return this; }
        KibBuilder newCredits(int count) { newCredits = count; return this; }
        KibBuilder openCredits(int count) { openCredits = count; return this; }
        KibBuilder loan(Loan loan) { loans = List.of(loan); return this; }
        KibBuilder episodes(List<OverdueEpisode> list) { episodes = list; return this; }
        KibBuilder contracts(List<Contract> list) { contracts = list; return this; }
        KibBuilder episodesUnavailable() { episodesUnavailable = true; return this; }
        KibBuilder missing(KibPart part) { missing = Set.of(part); return this; }

        Kib build() {
            return new Kib(activeDisputes, 0, bureauRequests, BigDecimal.ZERO, newCredits, openCredits,
                    loans, episodes, contracts, episodesUnavailable, null, missing);
        }
    }
}
