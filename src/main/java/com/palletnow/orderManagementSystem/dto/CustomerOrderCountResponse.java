package com.palletnow.orderManagementSystem.dto;

public record CustomerOrderCountResponse(Long customerId, String customerName, Long orderCount) {}
