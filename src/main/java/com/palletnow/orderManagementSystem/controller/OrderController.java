package com.palletnow.orderManagementSystem.controller;

import com.palletnow.orderManagementSystem.dto.*;
import com.palletnow.orderManagementSystem.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.place(request));
    }

    @GetMapping("/customer/{customerId}")
    public PageResponse<OrderResponse> byCustomer(@PathVariable Long customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(service.findByCustomer(customerId, pageable));
    }
}
