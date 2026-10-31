package kg.bakai.stopfactor.service.admin;

import kg.bakai.stopfactor.dto.admin.ProductDto;

import java.util.List;

public interface ProductAdminService {

    List<ProductDto> getAll();

    ProductDto get(long id);

    ProductDto create(ProductDto request);

    ProductDto update(long id, ProductDto request);

    void delete(long id);
}
