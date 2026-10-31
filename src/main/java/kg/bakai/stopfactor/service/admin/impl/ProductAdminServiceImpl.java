package kg.bakai.stopfactor.service.admin.impl;

import kg.bakai.stopfactor.dto.admin.ProductDto;
import kg.bakai.stopfactor.entity.Product;
import kg.bakai.stopfactor.exception.ConflictException;
import kg.bakai.stopfactor.exception.NotFoundException;
import kg.bakai.stopfactor.repository.ProductRepository;
import kg.bakai.stopfactor.repository.StopFactorCheckRepository;
import kg.bakai.stopfactor.repository.StopFactorRepository;
import kg.bakai.stopfactor.service.admin.ProductAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAdminServiceImpl implements ProductAdminService {

    private final ProductRepository productRepository;
    private final StopFactorRepository stopFactorRepository;
    private final StopFactorCheckRepository stopFactorCheckRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getAll() {
        return productRepository.findAll(Sort.by("id")).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto get(long id) {
        return toDto(find(id));
    }

    @Override
    @Transactional
    public ProductDto create(ProductDto request) {
        String name = request.name().trim();
        requireUniqueName(name, null);
        Product product = new Product();
        product.setName(name);
        product.setDescription(request.description().trim());
        Product saved = productRepository.save(product);
        log.info("Продукт {} '{}' создан", saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Override
    @Transactional
    public ProductDto update(long id, ProductDto request) {
        Product product = find(id);
        String name = request.name().trim();
        requireUniqueName(name, id);
        product.setName(name);
        product.setDescription(request.description().trim());
        log.info("Продукт {} обновлен", id);
        return toDto(productRepository.saveAndFlush(product));
    }

    @Override
    @Transactional
    public void delete(long id) {
        Product product = find(id);
        if (stopFactorRepository.isProductUsed(id) || stopFactorCheckRepository.existsByProduct_Id(id)) {
            throw new ConflictException("Продукт " + id + " используется стоп-факторами или проверками и не может быть удален");
        }
        productRepository.delete(product);
        log.info("Продукт {} '{}' удален", id, product.getName());
    }

    // Сравнение в Java: при локали C базы данных lower() в PostgreSQL не меняет кириллицу
    private void requireUniqueName(String name, Long exceptId) {
        boolean taken = productRepository.findAll().stream()
                .anyMatch(p -> !p.getId().equals(exceptId) && p.getName().equalsIgnoreCase(name));
        if (taken) {
            throw new ConflictException("Продукт '" + name + "' уже существует");
        }
    }

    private Product find(long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Продукт " + id + " не найден"));
    }

    private ProductDto toDto(Product product) {
        return new ProductDto(product.getId(), product.getName(), product.getDescription(),
                product.getCreatedDate(), product.getUpdatedDate());
    }
}
