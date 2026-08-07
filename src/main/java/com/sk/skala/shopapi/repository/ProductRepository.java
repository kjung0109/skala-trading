package com.sk.skala.shopapi.repository;

import com.sk.skala.shopapi.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductName(String productName);

    boolean existsByProductName(String productName);
}
