package kg.bakai.stopfactor.repository;

import kg.bakai.stopfactor.entity.StopFactor;
import kg.bakai.stopfactor.repository.projection.ProductProjection;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StopFactorRepository extends JpaRepository<StopFactor, Long> {

    /** Current versions of the active stop factors with their metrics, in display order. */
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

    // Every change of a stop factor first locks its first version, so concurrent edits run one after another.
    // The first version is locked, not the current one: the current one stops matching valid_to is null once an edit commits
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

    /** Whether a rule of any version of a stop factor of this metric has a value of this param. */
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM stop_factors.stop_factor_rule r
                           JOIN stop_factors.stop_factor sf ON sf.id = r.stop_factor_id
                           WHERE sf.metric_id = :metricId AND jsonb_exists(r.params, :key))
            """, nativeQuery = true)
    boolean isParamUsed(long metricId, String key);

    /**
     * Product of the application being scored. Empty if the scoring has no product (legacy scorings) -
     * then product limits are ignored, like before they existed, and the default segment is used.
     * Tables of SPR in the public schema: scoring(id, product_id), product(id, name, description, segment_type).
     */
    @Query(value = """
            SELECT p.id AS id, p.name AS name, p.description AS description, p.segment_type AS segmentType
            FROM public.scoring s
            JOIN public.product p ON p.id = s.product_id
            WHERE s.id = :scoringId
            """, nativeQuery = true)
    Optional<ProductProjection> findProductByScoringId(long scoringId);

    /**
     * Products of public.product (SPR) with these ids; ids without a product are not returned.
     */
    @Query(value = """
            SELECT p.id AS id, p.name AS name, p.description AS description, p.segment_type AS segmentType
            FROM public.product p
            WHERE p.id IN (:productIds)
            """, nativeQuery = true)
    List<ProductProjection> findProductsByIds(Collection<Long> productIds);

    @Query(value = "SELECT COALESCE(MAX(order_number), 0) FROM stop_factors.stop_factor WHERE valid_to IS NULL",
            nativeQuery = true)
    int findMaxCurrentOrderNumber();
}
