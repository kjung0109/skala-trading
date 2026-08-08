package com.sk.skala.trading.order;

import com.sk.skala.trading.order.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    /**
     * 종목별 최근 체결. 봇이 계속 거래하므로 전체를 내려주면 응답이 끝없이 커진다.
     * 체결 테이프에 필요한 만큼만 Pageable로 잘라 가져온다.
     */
    @Query("""
            select t from Trade t
            join fetch t.stock
            where t.stock.id = :stockId
            order by t.tradedAt desc
            """)
    List<Trade> findByStockId(@Param("stockId") Long stockId, Pageable pageable);

    /**
     * 가격 추이용 조회.
     *
     * 체결 테이프와 목적이 다르다. 테이프는 최근 몇 건만 보여주면 되지만
     * 차트는 시간 범위가 넓어야 추이가 보인다. 봇이 초당 수십 건을 체결시키므로
     * 같은 200건이어도 테이프에는 충분하고 차트에는 30초치밖에 되지 않는다.
     *
     * 엔티티 대신 시각·가격·수량만 뽑아 온다. 캔들을 만드는 데 필요한 것이 그뿐이라
     * 넓은 범위를 가져와도 부담이 적다.
     */
    @Query("""
            select t.tradedAt, t.price, t.quantity from Trade t
            where t.stock.id = :stockId
            order by t.tradedAt desc
            """)
    List<Object[]> findRecentTradePoints(@Param("stockId") Long stockId, Pageable pageable);

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
