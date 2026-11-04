package kg.bakai.stopfactor.testcheck;

import kg.bakai.common.enums.ProductSegment;
import kg.bakai.common.exception.NotFoundException;
import kg.bakai.spr.core.kib.dto.ConsolidationDto;
import kg.bakai.spr.core.kib.dto.GeneralInfoDto;
import kg.bakai.spr.core.kib.dto.IndividualReport;
import kg.bakai.spr.core.kib.dto.LoanDto;
import kg.bakai.spr.core.kib.dto.enums.LoanStatus;
import kg.bakai.spr.core.kib.dto.enums.RoleOfClient;
import kg.bakai.spr.core.kib.dto.response.CustomReport;
import kg.bakai.spr.core.ob.dto.TotalDebtReportData;
import kg.bakai.spr.core.ob.dto.response.TotalDebtFinalResponse;
import kg.bakai.stopfactor.dto.scoring.StopFactorDecision;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.repository.StopFactorCheckRepository;
import kg.bakai.stopfactor.repository.StopFactorRepository;
import kg.bakai.stopfactor.repository.projection.ProductProjection;
import kg.bakai.stopfactor.repository.projection.StopFactorProjection;
import kg.bakai.stopfactor.service.StopFactorService;
import kg.bakai.stopfactor.testcheck.TestCheckRequest.Kib;
import kg.bakai.stopfactor.testcheck.TestCheckRequest.KibPart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LOCAL TESTING ONLY. Builds the KIB / OB reports SPR would pass from a short request, imitates missing data
 * and calls {@link StopFactorService#check} exactly as SPR does. The results are saved like real ones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stop-factor.test-check.enabled", havingValue = "true")
public class StopFactorTestCheckService {

    // Test scorings without an id get negative ids, so they never clash with real scorings
    private final AtomicLong nextTestScoringId = new AtomicLong(-1_000_000_000L - System.currentTimeMillis() % 1_000_000_000L);

    private final StopFactorService stopFactorService;
    private final StopFactorRepository stopFactorRepository;
    private final StopFactorCheckRepository stopFactorCheckRepository;
    private final JdbcTemplate jdbcTemplate;

    public List<TestCheckScenario> scenarios() {
        return TestCheckScenarios.ALL;
    }

    @Transactional
    public TestCheckScenarioResult runScenario(String name) {
        TestCheckScenario scenario = TestCheckScenarios.ALL.stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Сценарий '" + name + "' не найден, список: GET /api/v1/test/stop-factor-check/scenarios"));
        TestCheckResponse result = check(scenario.request());
        boolean asExpected = result.decision() == scenario.expectedDecision()
                && result.internalStatusCode() == scenario.expectedInternalStatusCode();
        return new TestCheckScenarioResult(scenario.name(), scenario.description(), scenario.expectedDecision(),
                scenario.expectedInternalStatusCode(), asExpected, result);
    }

    @Transactional
    public TestCheckResponse check(TestCheckRequest request) {
        List<String> imitated = new ArrayList<>();
        long scoringId = request.scoringId() != null ? request.scoringId() : nextTestScoringId.getAndDecrement();
        if (request.productId() != null) {
            linkScoringToProduct(scoringId, request.productId());
        }

        Optional<ProductProjection> product = stopFactorRepository.findProductByScoringId(scoringId);
        ProductSegment segment = product.map(ProductProjection::getSegment).orElse(ProductSegment.DEFAULT);
        if (product.isEmpty()) {
            imitated.add("Скоринг " + scoringId + " без продукта: ограничения по продуктам не действуют, сегмент " + segment + " по умолчанию");
        }

        Kib kib = request.kib();
        CustomReport customReport = null;
        IndividualReport individualReport = null;
        ConsolidationDto consolidation = null;
        if (kib == null) {
            imitated.add("Нет отчёта КИБ: проверяются только данные ОБ, значения КИБ и история просрочек = 0");
        } else {
            Set<KibPart> missing = kib.missingParts() != null ? kib.missingParts() : Set.of();
            if (!missing.contains(KibPart.CUSTOM_REPORT)) {
                customReport = customReport(kib, imitated);
            }
            if (!missing.contains(KibPart.INDIVIDUAL_REPORT)) {
                individualReport = new IndividualReport(
                        new GeneralInfoDto(kib.activeDisputes(), kib.unjustifiedDisputes(), kib.bureauRequestsLast7Days()),
                        loans(kib));
            }
            if (!missing.contains(KibPart.CONSOLIDATION)) {
                consolidation = new ConsolidationDto(Objects.requireNonNullElse(kib.overdueAmount(), BigDecimal.ZERO),
                        kib.newCreditsLast30Days(), kib.openCredits());
            }
            if (!missing.isEmpty()) {
                imitated.add("Неполный отчёт КИБ, нет " + new TreeSet<>(missing)
                        + ": сервис считает, что отчёта КИБ нет, проверяются только данные ОБ");
            }
        }

        List<TotalDebtReportData> obCredits = request.obCredits() == null ? List.of() : request.obCredits().stream()
                .map(c -> new TotalDebtReportData(c.overdueDays(), c.overdueSum(), c.lastChangeDate()))
                .toList();
        if (obCredits.isEmpty()) {
            imitated.add("Нет кредитов в отчёте ОБ");
        }

        LocalDate requestDate = request.requestDate() != null ? request.requestDate() : LocalDate.now();
        StopFactorDecision decision = stopFactorService.check(scoringId, customReport, individualReport, consolidation,
                new TotalDebtFinalResponse(new ArrayList<>(obCredits)), requestDate);
        stopFactorCheckRepository.flush();
        List<StopFactorProjection> checks = stopFactorService.findByScoringId(scoringId);
        log.info("Тестовая проверка скоринга {}: {} {}", scoringId, decision.status(), decision.internalStatusCode());

        return new TestCheckResponse(
                scoringId,
                product.map(ProductProjection::getId).orElse(null),
                product.map(ProductProjection::getName).orElse(null),
                segment,
                decision.status(),
                decision.internalStatusCode(),
                imitated,
                checks.stream()
                        .filter(c -> c.getCheckResult() != StopFactorStatus.APPROVED)
                        .map(c -> c.getCheckResult() + ": " + c.getStopFactorName() + " (" + c.getCheckedValue() + ")")
                        .toList(),
                checks);
    }

    private static CustomReport customReport(Kib kib, List<String> imitated) {
        CustomReport report = new CustomReport();
        report.setOverdue(kib.legacyOverdue());
        if (kib.episodesUnavailable()) {
            report.setOverdueEpisodes(null);
            imitated.add("СПР не построил эпизоды просрочки: правила по просрочкам читают 17 старых счётчиков, "
                    + "диапазоны без счётчика дают NO_DATA, заявка уходит андеррайтеру");
        } else {
            report.setOverdueEpisodes(kib.overdueEpisodes() != null ? kib.overdueEpisodes() : List.of());
        }
        return report;
    }

    private static List<LoanDto> loans(Kib kib) {
        if (kib.loans() == null) {
            return List.of();
        }
        return kib.loans().stream()
                .map(l -> new LoanDto(
                        Objects.requireNonNullElse(l.role(), RoleOfClient.MainDebtor),
                        Objects.requireNonNullElse(l.status(), LoanStatus.Open),
                        l.restructuringsLast12Months(),
                        l.restructuringsFrom12To24Months(),
                        l.daysDelay()))
                .toList();
    }

    // public.scoring belongs to SPR; here the test application needs a scoring row to find its product
    private void linkScoringToProduct(long scoringId, long productId) {
        if (stopFactorRepository.findProductsByIds(List.of(productId)).isEmpty()) {
            throw new NotFoundException("Продукт " + productId + " не найден");
        }
        jdbcTemplate.update("""
                INSERT INTO public.scoring (id, created_date, product_id) VALUES (?, now(), ?)
                ON CONFLICT (id) DO UPDATE SET product_id = EXCLUDED.product_id
                """, scoringId, productId);
    }
}
