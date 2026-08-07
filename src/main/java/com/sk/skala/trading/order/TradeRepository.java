package com.sk.skala.trading.order;

import com.sk.skala.trading.order.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    @Query("""
            select t from Trade t
            join fetch t.stock
            where t.stock.id = :stockId
            order by t.tradedAt desc
            """)
    List<Trade> findByStockId(@Param("stockId") Long stockId);

    /** 내 체결 내역. 매수·매도 어느 쪽이든 내 주문이면 포함한다. */
    @Query("""
            select t from Trade t
            join fetch t.stock
            join fetch t.buyOrder bo
            join fetch t.sellOrder so
            where bo.account.accountId = :accountId or so.account.accountId = :accountId
            order by t.tradedAt desc
            """)
    Page<Trade> findByAccountId(@Param("accountId") String accountId, Pageable pageable);
}
