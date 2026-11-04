package kg.bakai.product.repository;

import kg.bakai.product.entity.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Read-only: exposes no save or delete, products belong to SPR.
 */
public interface ProductRepository extends Repository<Product, Long> {

    List<Product> findAll(Sort sort);

    Optional<Product> findById(Long id);
}
