package kg.bakai.product.service;

import kg.bakai.product.dto.ProductDto;

import java.util.List;

/**
 * Read-only access to the credit products (public.product). Products and their segments are managed by SPR,
 * not through this service.
 */
public interface ProductService {

    List<ProductDto> getAll();

    ProductDto get(long id);
}
