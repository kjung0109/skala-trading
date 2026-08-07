package com.sk.skala.shopapi.repository;

import com.sk.skala.shopapi.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
}
