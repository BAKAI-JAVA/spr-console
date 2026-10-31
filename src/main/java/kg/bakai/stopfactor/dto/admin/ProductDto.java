package kg.bakai.stopfactor.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ProductDto(
        Long id,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String description,
        LocalDateTime createdDate,
        LocalDateTime updatedDate
) {
}
