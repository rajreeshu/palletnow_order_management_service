package com.palletnow.orderManagementSystem.service;

import com.palletnow.orderManagementSystem.dto.*;
import com.palletnow.orderManagementSystem.entity.*;
import com.palletnow.orderManagementSystem.exception.*;
import com.palletnow.orderManagementSystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse place(OrderRequest request) {
        Customer customer = customerService.find(request.customerId());
        Map<Long, Integer> quantities = request.items().stream().collect(Collectors.groupingBy(
                OrderLineRequest::productId, LinkedHashMap::new,
                Collectors.summingInt(OrderLineRequest::quantity)));
        Map<Long, Product> products = new TreeMap<>();
        for (Long productId : quantities.keySet()) {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
            int quantity = quantities.get(productId);
            if (product.getStock() < quantity) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }
            products.put(productId, product);
        }
        CustomerOrder order = new CustomerOrder(customer);
        quantities.forEach((id, quantity) -> {
            Product product = products.get(id);
            product.deductStock(quantity);
            order.addItem(product, quantity);
        });
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findByCustomer(Long customerId, Pageable pageable) {
        customerService.find(customerId);
        return orderRepository.findByCustomerId(customerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CustomerOrderCountResponse> report(Pageable pageable) {
        return customerRepository.countOrdersByCustomer(pageable).map(c ->
                new CustomerOrderCountResponse(c.getCustomerId(), c.getCustomerName(), c.getOrderCount()));
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderCountResponse> topCustomers() {
        return report(PageRequest.of(0, 5,
                Sort.by(Sort.Direction.DESC, "orderCount")
                        .and(Sort.by(Sort.Direction.ASC, "customerId")))).getContent();
    }

    private OrderResponse toResponse(CustomerOrder order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderResponse.OrderItemResponse(i.getProduct().getId(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity(), i.getLineTotal())).toList();
        return new OrderResponse(order.getId(), order.getCustomer().getId(), order.getCustomer().getName(),
                order.getCreatedAt(), items, order.totalAmount());
    }
}
