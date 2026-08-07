package com.sk.skala.trading.account;

import com.sk.skala.trading.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}
