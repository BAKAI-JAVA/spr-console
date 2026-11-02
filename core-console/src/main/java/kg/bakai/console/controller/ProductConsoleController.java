package kg.bakai.console.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.bakai.console.service.ProductConsoleService;
import kg.bakai.product.dto.ProductDto;
import kg.bakai.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Console: products", description = "Credit products stop factors can be limited to")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
public class ProductConsoleController {

    private final ProductService productService;
    private final ProductConsoleService productConsoleService;

    @GetMapping
    public List<ProductDto> getAll() {
        return productService.getAll();
    }

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable long id) {
        return productService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody ProductDto request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductDto update(@PathVariable long id, @Valid @RequestBody ProductDto request) {
        return productService.update(id, request);
    }

    @Operation(summary = "Delete a product", description = "Only possible while no stop factor version or check refers to it")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        productConsoleService.delete(id);
    }
}
