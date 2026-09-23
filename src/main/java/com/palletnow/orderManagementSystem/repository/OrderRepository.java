package com.palletnow.orderManagementSystem.repository;

import com.palletnow.orderManagementSystem.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.List;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    Page<CustomerOrder> findByCustomerId(Long customerId, Pageable pageable);
}
