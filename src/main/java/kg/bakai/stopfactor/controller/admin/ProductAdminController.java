package kg.bakai.stopfactor.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.bakai.stopfactor.dto.admin.ProductDto;
import kg.bakai.stopfactor.service.admin.ProductAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin: products", description = "Credit products stop factors can be limited to")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
public class ProductAdminController {

    private final ProductAdminService productAdminService;

    @GetMapping
    public List<ProductDto> getAll() {
        return productAdminService.getAll();
    }

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable long id) {
        return productAdminService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody ProductDto request) {
        return productAdminService.create(request);
    }

    @PutMapping("/{id}")
    public ProductDto update(@PathVariable long id, @Valid @RequestBody ProductDto request) {
        return productAdminService.update(id, request);
    }

    @Operation(summary = "Delete a product", description = "Only possible while no stop factor version or check refers to it")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        productAdminService.delete(id);
    }
}
