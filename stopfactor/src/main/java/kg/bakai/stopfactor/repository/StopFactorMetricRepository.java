package kg.bakai.stopfactor.repository;

import kg.bakai.stopfactor.entity.StopFactorMetric;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StopFactorMetricRepository extends JpaRepository<StopFactorMetric, Long> {

    /**
     * Metric by its title ("Активные споры КИБ") or name template ("Количество активных споров КИБ").
     * Used by the legacy StopFactorService.save(name, description), which knows only the stop factor name.
     */
    Optional<StopFactorMetric> findFirstByTitleOrNameTemplateOrderById(String title, String nameTemplate);
}
