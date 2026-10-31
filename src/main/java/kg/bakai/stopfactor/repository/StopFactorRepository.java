package kg.bakai.stopfactor.repository;

import kg.bakai.stopfactor.entity.StopFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StopFactorRepository extends JpaRepository<StopFactor, Long> {

    @Query(value = """
            SELECT * FROM stop_factors.stop_factor
            WHERE is_active = true
            ORDER BY order_number
            """, nativeQuery = true)
    List<StopFactor> findAllActive();
}