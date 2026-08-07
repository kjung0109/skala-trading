package com.sk.skala.shopapi.repository;

import com.sk.skala.shopapi.domain.Account;
import com.sk.skala.shopapi.domain.Holding;
import com.sk.skala.shopapi.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    /** 보유 목록. stock을 fetch join으로 함께 읽어 N+1을 피한다. */
    @Query("""
            select h from Holding h
            join fetch h.stock
            where h.account.accountId = :accountId and h.quantity > 0
            """)
    List<Holding> findByAccountId(@Param("accountId") String accountId);

    Optional<Holding> findByAccountAndStock(Account account, Stock stock);
}
