package kg.bakai.stopfactor.repository;

import jakarta.persistence.LockModeType;
import kg.bakai.stopfactor.entity.StopFactor;
import kg.bakai.stopfactor.repository.projection.ProductProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StopFactorRepository extends JpaRepository<StopFactor, Long> {

    @Query(value = """
            SELECT * FROM stop_factors.stop_factor
            WHERE is_active = true
              AND valid_to IS NULL
            ORDER BY order_number, id
            """, nativeQuery = true)
    List<StopFactor> findAllActive();

    @Query("""
            select sf from StopFactor sf
            join fetch sf.metric
            where sf.validTo is null and sf.isActive = true
            order by sf.orderNumber, sf.id
            """)
    List<StopFactor> findCurrentActiveWithMetric();

    @Query("""
            select sf from StopFactor sf
            join fetch sf.metric
            where sf.validTo is null
            order by sf.orderNumber, sf.id
            """)
    List<StopFactor> findAllCurrentWithMetric();

    @Query("""
            select sf from StopFactor sf
            join fetch sf.metric
            where sf.validTo is null and ((sf.id = :factorId and sf.rootId is null) or sf.rootId = :factorId)
            """)
    Optional<StopFactor> findCurrent(long factorId);

    // Все изменения стоп-фактора сначала блокируют его первую версию, поэтому параллельные правки выполняются по очереди.
    // Блокируется первая версия, а не текущая: текущая перестает подходить под valid_to is null после коммита правки
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sf from StopFactor sf where sf.id = :factorId and sf.rootId is null")
    Optional<StopFactor> lockFactor(long factorId);

    @Query("""
            select sf from StopFactor sf
            join fetch sf.metric
            where (sf.id = :factorId and sf.rootId is null) or sf.rootId = :factorId
            order by sf.versionNumber desc
            """)
    List<StopFactor> findVersions(long factorId);

    @Query("select sf from StopFactor sf where sf.validTo is null and sf.metric.id = :metricId")
    List<StopFactor> findCurrentByMetricId(long metricId);

    boolean existsByMetric_Id(long metricId);

    /**
     * Product of the application being scored. Empty if the scoring has no product (legacy scorings) -
     * then every active stop factor is checked, like before product limits existed.
     * Tables of SPR in the public schema: scoring(id, product_id), product(id, name, description).
     */
    @Query(value = """
            SELECT p.id AS id, p.name AS name, p.description AS description
            FROM public.scoring s
            JOIN public.product p ON p.id = s.product_id
            WHERE s.id = :scoringId
            """, nativeQuery = true)
    Optional<ProductProjection> findProductByScoringId(long scoringId);

    /**
     * Products of public.product (SPR) with these ids; ids without a product are not returned.
     */
    @Query(value = """
            SELECT p.id AS id, p.name AS name, p.description AS description
            FROM public.product p
            WHERE p.id IN (:productIds)
            """, nativeQuery = true)
    List<ProductProjection> findProductsByIds(Collection<Long> productIds);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM stop_factors.stop_factor_product WHERE product_id = :productId)",
            nativeQuery = true)
    boolean isProductUsed(long productId);

    @Query(value = "SELECT COALESCE(MAX(order_number), 0) FROM stop_factors.stop_factor WHERE valid_to IS NULL",
            nativeQuery = true)
    int findMaxCurrentOrderNumber();
}
