package com.sk.skala.shopapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 체결 내역. 매수 주문과 매도 주문이 만나 성사된 거래 한 건이다.
 */
@Entity
@Table(name = "trade")
@Getter
@NoArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buy_order_id")
    private Order buyOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sell_order_id")
    private Order sellOrder;

    /** 체결 가격. 먼저 호가창에 있던 주문의 가격으로 체결된다. */
    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false)
    private LocalDateTime tradedAt;

    public Trade(Stock stock, Order buyOrder, Order sellOrder, Long price, Long quantity) {
        this.stock = stock;
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
        this.price = price;
        this.quantity = quantity;
        this.tradedAt = LocalDateTime.now();
    }

    public long amount() {
        return price * quantity;
    }
}
