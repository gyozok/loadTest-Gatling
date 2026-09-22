package dev.gyozok.loadtest.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderRequest(
        @NotBlank(message = "item must not be blank")
        String item,

        @Min(value = 1, message="quantity must be at least 1")
        int quantity

) {
}
