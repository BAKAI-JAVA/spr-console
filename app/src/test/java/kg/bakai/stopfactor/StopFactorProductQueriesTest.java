package kg.bakai.stopfactor;

import kg.bakai.common.exception.BusinessValidationException;
import kg.bakai.common.exception.ConflictException;
import kg.bakai.product.port.ProductUsageChecker;
import kg.bakai.product.service.ProductService;
import kg.bakai.stopfactor.dto.console.ConditionDto;
import kg.bakai.stopfactor.dto.console.StopFactorRequest;
import kg.bakai.stopfactor.dto.console.StopFactorResponse;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import kg.bakai.stopfactor.enums.StopFactorValueTypes;
import kg.bakai.stopfactor.repository.StopFactorRepository;
import kg.bakai.stopfactor.repository.projection.ProductProjection;
import kg.bakai.stopfactor.service.console.StopFactorConsoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Native product queries of the stop factor module (public.product read as ProductProjection, no core-online
 * classes) and the product usage check core-console gives to core-online. Runs against the database, every test
 * is rolled back. Test products have negative ids so they never clash with real ones.
 */
@SpringBootTest
@Transactional
class StopFactorProductQueriesTest {

    private static final long PRODUCT_A = -1;
    private static final long PRODUCT_B = -2;
    private static final long UNKNOWN_PRODUCT = -999;
    private static final long ACTIVE_DISPUTES_METRIC = 2;

    @Autowired
    private StopFactorRepository stopFactorRepository;
    @Autowired
    private StopFactorConsoleService stopFactorConsoleService;
    @Autowired
    private ProductUsageChecker productUsageChecker;
    @Autowired
    private ProductService productService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void products() {
        jdbcTemplate.update("INSERT INTO public.product (id, name, description) VALUES (?, 'Test A', 'Product A'), (?, 'Test B', 'Product B')",
                PRODUCT_A, PRODUCT_B);
    }

    // ------------------------------------------------------------------------------------------------
    // StopFactorRepository native queries
    // ------------------------------------------------------------------------------------------------

    @Test
    void findProductsByIdsReturnsOnlyExistingProductsWithAllColumns() {
        List<ProductProjection> products = stopFactorRepository.findProductsByIds(List.of(PRODUCT_A, PRODUCT_B, UNKNOWN_PRODUCT));

        assertThat(products)
                .extracting(ProductProjection::getId, ProductProjection::getName, ProductProjection::getDescription)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(PRODUCT_A, "Test A", "Product A"),
                        org.assertj.core.groups.Tuple.tuple(PRODUCT_B, "Test B", "Product B"));
    }

    @Test
    void findProductsByIdsWithoutExistingProductsIsEmpty() {
        assertThat(stopFactorRepository.findProductsByIds(List.of(UNKNOWN_PRODUCT))).isEmpty();
    }

    @Test
    void findProductByScoringIdReadsTheProductOfTheScoring() {
        jdbcTemplate.update("INSERT INTO public.scoring (id, product_id) VALUES (-500, ?), (-501, NULL)", PRODUCT_B);

        assertThat(stopFactorRepository.findProductByScoringId(-500))
                .get()
                .extracting(ProductProjection::getId, ProductProjection::getName, ProductProjection::getDescription)
                .containsExactly(PRODUCT_B, "Test B", "Product B");
        assertThat(stopFactorRepository.findProductByScoringId(-501)).isEmpty();   // scoring without product
        assertThat(stopFactorRepository.findProductByScoringId(-502)).isEmpty();   // unknown scoring (legacy)
    }

    // ------------------------------------------------------------------------------------------------
    // Console validation of product ids (StopFactorDefinitionValidator uses findProductsByIds)
    // ------------------------------------------------------------------------------------------------

    @Test
    void stopFactorCanBeLimitedToExistingProducts() {
        StopFactorResponse created = stopFactorConsoleService.create(stopFactorRequest(Set.of(PRODUCT_A, PRODUCT_B)), "test");

        assertThat(created.productIds()).containsExactlyInAnyOrder(PRODUCT_A, PRODUCT_B);
        assertThat(stopFactorRepository.isProductUsed(PRODUCT_A)).isTrue();
    }

    @Test
    void unknownProductIdIsRejectedByTheConsole() {
        assertThatThrownBy(() -> stopFactorConsoleService.create(stopFactorRequest(Set.of(PRODUCT_A, UNKNOWN_PRODUCT)), "test"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("productIds: продукт " + UNKNOWN_PRODUCT + " не существует")
                .hasMessageNotContaining("продукт " + PRODUCT_A + " ");
    }

    // ------------------------------------------------------------------------------------------------
    // ProductUsageChecker (core-console) used by core-online before deleting a product
    // ------------------------------------------------------------------------------------------------

    @Test
    void productNotUsedByStopFactorsCanBeDeleted() {
        assertThat(productUsageChecker.isProductUsed(PRODUCT_A)).isFalse();

        productService.delete(PRODUCT_A);

        assertThat(stopFactorRepository.findProductsByIds(List.of(PRODUCT_A))).isEmpty();
    }

    @Test
    void productOfAStopFactorCannotBeDeleted() {
        jdbcTemplate.update("INSERT INTO stop_factors.stop_factor_product (stop_factor_id, product_id) VALUES (2, ?)", PRODUCT_A);

        assertThat(productUsageChecker.isProductUsed(PRODUCT_A)).isTrue();
        assertThatThrownBy(() -> productService.delete(PRODUCT_A)).isInstanceOf(ConflictException.class);
    }

    @Test
    void productOfASavedCheckCannotBeDeleted() {
        // only a check refers to the product, no stop factor is limited to it
        jdbcTemplate.update("""
                INSERT INTO stop_factors.stop_factor_check (id, scoring_id, stop_factor_id, checked_value, result, product_id)
                VALUES (nextval('stop_factors.stop_factor_check_id_generator'), -500, 2, '0.0', 'APPROVED', ?)
                """, PRODUCT_B);

        assertThat(stopFactorRepository.isProductUsed(PRODUCT_B)).isFalse();
        assertThat(productUsageChecker.isProductUsed(PRODUCT_B)).isTrue();
        assertThatThrownBy(() -> productService.delete(PRODUCT_B)).isInstanceOf(ConflictException.class);
    }

    private static StopFactorRequest stopFactorRequest(Set<Long> productIds) {
        return new StopFactorRequest(ACTIVE_DISPUTES_METRIC, "Тест: активные споры по продукту", null, null, true,
                Map.of(), productIds,
                List.of(new ConditionDto(null, StopFactorStatus.REJECTED, StopFactorValueTypes.INTEGER, "0")),
                null);
    }
}
