package com.palletnow.orderManagementSystem.service;

import com.palletnow.orderManagementSystem.dto.*;
import com.palletnow.orderManagementSystem.entity.Product;
import com.palletnow.orderManagementSystem.exception.ResourceNotFoundException;
import com.palletnow.orderManagementSystem.repository.ProductRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.data.domain.*;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repository;

    public ProductResponse create(ProductRequest r) {
        return toResponse(repository.save(Product.builder()
                .name(r.name())
                .price(r.price())
                .stock(r.stock())
                .build()));
    }

    public ProductResponse update(Long id, ProductRequest r) {
        Product p = find(id);
        p.update(r.name(), r.price(), r.stock());
        return toResponse(repository.save(p));
    }

    public Page<ProductResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    public ProductResponse findById(Long id) {
        return toResponse(find(id));
    }

    public Product find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getPrice(), p.getStock());
    }
}
