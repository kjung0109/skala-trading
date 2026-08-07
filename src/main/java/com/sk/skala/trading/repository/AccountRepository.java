package com.sk.skala.trading.repository;

import com.sk.skala.trading.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}
