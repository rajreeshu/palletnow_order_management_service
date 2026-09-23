package com.palletnow.orderManagementSystem.service;

import com.palletnow.orderManagementSystem.dto.*;
import com.palletnow.orderManagementSystem.entity.Customer;
import com.palletnow.orderManagementSystem.exception.*;
import com.palletnow.orderManagementSystem.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.data.domain.*;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository repository;

    public CustomerResponse create(CustomerRequest request) {
        if (repository.existsByEmail(request.email())) throw new ConflictException("Email is already in use");
        if (repository.existsByPhone(request.phone())) throw new ConflictException("Phone is already in use");
        return toResponse(repository.save(Customer.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .build()));
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = find(id);
        if (repository.existsByEmailAndIdNot(request.email(), id))
            throw new ConflictException("Email is already in use");
        if (repository.existsByPhoneAndIdNot(request.phone(), id))
            throw new ConflictException("Phone is already in use");
        customer.update(request.name(), request.email(), request.phone());
        return toResponse(repository.save(customer));
    }

    public Page<CustomerResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    public CustomerResponse findById(Long id) {
        return toResponse(find(id));
    }

    public Customer find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(c.getId(), c.getName(), c.getEmail(), c.getPhone());
    }
}
