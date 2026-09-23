package com.palletnow.orderManagementSystem.dto;

import jakarta.validation.constraints.*;

public record CustomerRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String phone) {}
