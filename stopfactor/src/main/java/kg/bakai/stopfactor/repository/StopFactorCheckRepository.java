package kg.bakai.stopfactor.repository;

import kg.bakai.stopfactor.entity.StopFactorCheck;
import kg.bakai.stopfactor.dto.StopFactorProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StopFactorCheckRepository extends JpaRepository<StopFactorCheck, Long> {

    /** Results of a scoring, one per rule, with the conditions of the rule of the version each was made with. */
    @Query(value = """
            select
                coalesce(sf.root_id, sf.id) as "stopFactorId",
                sf.id as "stopFactorVersionId",
                sf.version_number as "versionNumber",
                sf.name as "stopFactorName",
                r.id as "ruleId",
                r.name as "ruleName",
                c.result as "checkResult",
                c.checked_value as "checkedValue",
                rej.value as "rejectedValue",
                rej.value_type as "rejectedValueType",
                rej.operator as "rejectedOperator",
                uw.value as "underwriterValue",
                uw.value_type as "underwriterValueType",
                uw.operator as "underwriterOperator",
                uw.value_to as "underwriterValueTo"
            from stop_factors.stop_factor_check c
            join stop_factors.stop_factor_rule r on r.id = c.rule_id
            join stop_factors.stop_factor sf on sf.id = r.stop_factor_id
            left join stop_factors.stop_factor_condition rej on rej.rule_id = r.id and rej.result = 'REJECTED'
            left join stop_factors.stop_factor_condition uw on uw.rule_id = r.id and uw.result = 'UNDERWRITER'
            where c.scoring_id = :scoringId
            order by sf.order_number, sf.id, r.order_number
            """, nativeQuery = true)
    List<StopFactorProjection> findByScoringIdForProjection(long scoringId);

    @Modifying
    @Query("delete from StopFactorCheck c where c.scoringId = :scoringId")
    void deleteByScoringId(long scoringId);
}