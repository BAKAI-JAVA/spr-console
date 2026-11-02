package kg.bakai.console.service;

import kg.bakai.common.exception.ConflictException;
import kg.bakai.product.service.ProductService;
import kg.bakai.stopfactor.service.StopFactorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product operations that need both modules: product does not know about stopfactor,
 * so the usage check before deleting is done here.
 */
@Service
@RequiredArgsConstructor
public class ProductConsoleService {

    private final ProductService productService;
    private final StopFactorService stopFactorService;

    // One transaction for the usage check and the delete
    @Transactional
    public void delete(long id) {
        if (stopFactorService.isProductUsed(id)) {
            throw new ConflictException("Продукт " + id + " используется стоп-факторами или проверками и не может быть удален");
        }
        productService.delete(id);
    }
}
