package com.sk.skala.shopapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 보유 종목. 체결될 때마다 수량과 평균 단가가 갱신된다.
 */
@Entity
@Table(name = "holding",
        uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "stock_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Column(nullable = false)
    private Long quantity;

    /** 평균 매입 단가 */
    @Column(nullable = false)
    private Long averagePrice;

    public Holding(Account account, Stock stock, Long quantity, Long averagePrice) {
        this.account = account;
        this.stock = stock;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
    }

    /** 매수 체결. 기존 보유분과 합쳐 평균 단가를 다시 계산한다. */
    public void addBuy(long addQuantity, long price) {
        long totalCost = averagePrice * quantity + price * addQuantity;
        this.quantity += addQuantity;
        this.averagePrice = totalCost / this.quantity;
    }

    /** 매도 체결. 평균 단가는 그대로 두고 수량만 줄인다. */
    public void reduce(long soldQuantity) {
        this.quantity -= soldQuantity;
    }

    public boolean isEmpty() {
        return quantity <= 0;
    }
}
