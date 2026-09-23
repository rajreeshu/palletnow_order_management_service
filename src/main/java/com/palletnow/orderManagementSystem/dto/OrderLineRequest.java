package com.palletnow.orderManagementSystem.dto;

import jakarta.validation.constraints.*;

public record OrderLineRequest(@NotNull Long productId, @NotNull @Positive Integer quantity) {}
