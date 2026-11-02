package kg.bakai.stopfactor.repository;

import jakarta.persistence.LockModeType;
import kg.bakai.stopfactor.entity.StopFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

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

    @Query(value = "SELECT EXISTS (SELECT 1 FROM stop_factors.stop_factor_product WHERE product_id = :productId)",
            nativeQuery = true)
    boolean isProductUsed(long productId);

    @Query(value = "SELECT COALESCE(MAX(order_number), 0) FROM stop_factors.stop_factor WHERE valid_to IS NULL",
            nativeQuery = true)
    int findMaxCurrentOrderNumber();
}
