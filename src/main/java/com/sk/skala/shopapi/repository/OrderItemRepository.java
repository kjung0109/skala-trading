package com.sk.skala.shopapi.repository;

import com.sk.skala.shopapi.domain.Customer;
import com.sk.skala.shopapi.domain.OrderItem;
import com.sk.skala.shopapi.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 고객의 주문 목록.
     * product를 fetch join으로 함께 읽는다. 이게 없으면 주문 건수만큼
     * product 조회 쿼리가 추가로 나간다(N+1).
     */
    @Query("select oi from OrderItem oi join fetch oi.product where oi.customer.customerId = :customerId")
    List<OrderItem> findByCustomerCustomerId(String customerId);

    Optional<OrderItem> findByCustomerAndProduct(Customer customer, Product product);

    boolean existsByProduct(Product product);
}
