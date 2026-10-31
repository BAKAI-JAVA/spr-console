package kg.bakai.stopfactor.repository;

import kg.bakai.stopfactor.entity.StopFactorCondition;
import kg.bakai.stopfactor.enums.StopFactorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StopFactorConditionRepository extends JpaRepository<StopFactorCondition, Long> {
    Optional<StopFactorCondition> findByStopFactor_IdAndResult(Long id, StopFactorStatus result);

    List<StopFactorCondition> findAllByStopFactor_IdIn(Collection<Long> stopFactorIds);

    List<StopFactorCondition> findAllByStopFactor_Id(Long stopFactorId);
}
