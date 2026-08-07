package com.sk.skala.shopapi.repository;

import com.sk.skala.shopapi.domain.Order;
import com.sk.skala.shopapi.domain.OrderSide;
import com.sk.skala.shopapi.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 매수 주문과 맞을 수 있는 매도 주문을 찾는다.
     * 가격 우선(싼 매도 먼저) → 시간 우선(먼저 낸 주문 먼저).
     */
    @Query("""
            select o from Order o
            join fetch o.account
            where o.stock.id = :stockId
              and o.side = com.sk.skala.shopapi.domain.OrderSide.SELL
              and o.status in (com.sk.skala.shopapi.domain.OrderStatus.OPEN,
                               com.sk.skala.shopapi.domain.OrderStatus.PARTIALLY_FILLED)
              and o.price <= :price
              and o.account.accountId <> :accountId
            order by o.price asc, o.createdAt asc
            """)
    List<Order> findMatchableSellOrders(@Param("stockId") Long stockId,
                                        @Param("price") Long price,
                                        @Param("accountId") String accountId);

    /**
     * 매도 주문과 맞을 수 있는 매수 주문.
     * 가격 우선(비싼 매수 먼저) → 시간 우선.
     */
    @Query("""
            select o from Order o
            join fetch o.account
            where o.stock.id = :stockId
              and o.side = com.sk.skala.shopapi.domain.OrderSide.BUY
              and o.status in (com.sk.skala.shopapi.domain.OrderStatus.OPEN,
                               com.sk.skala.shopapi.domain.OrderStatus.PARTIALLY_FILLED)
              and o.price >= :price
              and o.account.accountId <> :accountId
            order by o.price desc, o.createdAt asc
            """)
    List<Order> findMatchableBuyOrders(@Param("stockId") Long stockId,
                                       @Param("price") Long price,
                                       @Param("accountId") String accountId);

    /** 호가창: 가격대별 잔량 합계 */
    @Query("""
            select o.price, sum(o.remainingQuantity)
            from Order o
            where o.stock.id = :stockId
              and o.side = :side
              and o.status in (com.sk.skala.shopapi.domain.OrderStatus.OPEN,
                               com.sk.skala.shopapi.domain.OrderStatus.PARTIALLY_FILLED)
            group by o.price
            """)
    List<Object[]> aggregateOrderBook(@Param("stockId") Long stockId, @Param("side") OrderSide side);

    @Query("""
            select o from Order o
            join fetch o.stock
            where o.account.accountId = :accountId
            order by o.createdAt desc
            """)
    Page<Order> findByAccountId(@Param("accountId") String accountId, Pageable pageable);

    List<Order> findByAccountAccountIdAndStatusIn(String accountId, List<OrderStatus> statuses);
}
