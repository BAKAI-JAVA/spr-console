package kg.bakai.product.service;

import kg.bakai.product.dto.ProductDto;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ProductService {

    List<ProductDto> getAll();

    ProductDto get(long id);

    ProductDto create(ProductDto request);

    ProductDto update(long id, ProductDto request);

    /**
     * Deletes the product without checking who refers to it: callers that know about
     * stop factors (core-console) must check usage first.
     */
    void delete(long id);

    boolean exists(long id);

    /**
     * @return the subset of {@code ids} that exist
     */
    Set<Long> findExistingIds(Collection<Long> ids);
}
