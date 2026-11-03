package kg.bakai.product.service;

import kg.bakai.product.dto.ProductDto;

import java.util.List;

public interface ProductService {

    List<ProductDto> getAll();

    ProductDto get(long id);

    ProductDto create(ProductDto request);

    ProductDto update(long id, ProductDto request);

    /**
     * Refuses to delete a product used by stop factors or their checks.
     */
    void delete(long id);
}
