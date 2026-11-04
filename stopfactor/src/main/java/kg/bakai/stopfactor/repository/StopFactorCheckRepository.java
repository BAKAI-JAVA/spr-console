package kg.bakai.stopfactor.repository;

import kg.bakai.stopfactor.entity.StopFactorCheck;
import kg.bakai.stopfactor.repository.projection.StopFactorProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StopFactorCheckRepository extends JpaRepository<StopFactorCheck, Long> {

    @Query(value = """
            select
                coalesce(sf.root_id, sf.id) as "stopFactorId",
                sf.id as "stopFactorVersionId",
                sf.version_number as "versionNumber",
                sf.name as "stopFactorName",
                c.result as "checkResult",
                c.checked_value as "checkedValue",
                sfc.value as "rejectedValue",
                sfc.value_type as "rejectedValueType",
                sfc2.value as "underwriterValue",
                sfc2.value_type as "underwriterValueType"
            from stop_factors.stop_factor_check c
            join stop_factors.stop_factor sf on sf.id = c.stop_factor_id
            left join stop_factors.stop_factor_condition sfc on sf.id = sfc.stop_factor_id and sfc.result = 'REJECTED'
            left join stop_factors.stop_factor_condition sfc2 on sf.id = sfc2.stop_factor_id and sfc2.result = 'UNDERWRITER'
            where c.scoring_id = :scoringId
            order by sf.order_number, sf.id
            """, nativeQuery = true)
    List<StopFactorProjection> findByScoringIdForProjection(long scoringId);

    @Modifying
    @Query("delete from StopFactorCheck c where c.scoringId = :scoringId")
    void deleteByScoringId(long scoringId);
}