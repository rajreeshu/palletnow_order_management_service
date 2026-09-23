package com.palletnow.orderManagementSystem.repository;

import com.palletnow.orderManagementSystem.entity.Customer;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByPhoneAndIdNot(String phone, Long id);
    @Query(value = """
            select c.id as customerId, c.name as customerName, count(o.id) as orderCount
            from customers c left join orders o on o.customer_id = c.id
            group by c.id, c.name
            """,
            countQuery = "select count(*) from customers",
            nativeQuery = true)
    Page<CustomerOrderCount> countOrdersByCustomer(Pageable pageable);

    interface CustomerOrderCount {
        Long getCustomerId();
        String getCustomerName();
        Long getOrderCount();
    }
}
