package com.sk.skala.shopapi.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상장 종목.
 * currentPrice는 마지막으로 체결된 가격이다. 체결이 일어날 때마다 갱신된다.
 */
@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    /** 최종 체결가 */
    @Column(nullable = false)
    private Long currentPrice;

    /** 전일 종가 */
    @Column(nullable = false)
    private Long previousPrice;

    public Stock(String code, String name, Long currentPrice, Long previousPrice) {
        this.code = code;
        this.name = name;
        this.currentPrice = currentPrice;
        this.previousPrice = previousPrice;
    }

    public void applyTradePrice(long price) {
        this.currentPrice = price;
    }
}
