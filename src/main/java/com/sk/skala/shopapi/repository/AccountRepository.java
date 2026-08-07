package com.sk.skala.shopapi.repository;

import com.sk.skala.shopapi.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}
