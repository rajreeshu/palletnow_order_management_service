package com.palletnow.orderManagementSystem.controller;

import com.palletnow.orderManagementSystem.dto.CustomerOrderCountResponse;
import com.palletnow.orderManagementSystem.dto.PageResponse;
import com.palletnow.orderManagementSystem.service.OrderService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {
    private final OrderService service;

    @GetMapping("/orders-by-customer")
    public PageResponse<CustomerOrderCountResponse> ordersByCustomer(
            @PageableDefault(size = 20, sort = "customerId", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(service.report(pageable));
    }

    @GetMapping("/top-customers")
    public List<CustomerOrderCountResponse> topCustomers() {
        return service.topCustomers();
    }
}
