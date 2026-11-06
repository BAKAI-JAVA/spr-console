package kg.bakai.stopfactor.repository;

import kg.bakai.stopfactor.entity.StopFactorCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StopFactorConditionRepository extends JpaRepository<StopFactorCondition, Long> {
    List<StopFactorCondition> findAllByStopFactor_IdIn(Collection<Long> stopFactorIds);

    List<StopFactorCondition> findAllByStopFactor_Id(Long stopFactorId);
}
