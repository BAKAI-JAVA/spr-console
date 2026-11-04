package kg.bakai.console.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import kg.bakai.product.dto.ProductDto;
import kg.bakai.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Console: products", description = "Credit products, read only (managed by SPR). segmentType (RETAIL / MSB) "
        + "decides which segment stop factors check their applications")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
public class ProductConsoleController {

    private final ProductService productService;

    @GetMapping
    public List<ProductDto> getAll() {
        return productService.getAll();
    }

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable long id) {
        return productService.get(id);
    }
}
