package com.sk.skala.shopapi.domain;

import com.sk.skala.shopapi.common.Error;
import com.sk.skala.shopapi.exception.ResponseException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객. 식별자는 자동 증가 값이 아니라 고객이 정한 문자열 ID다.
 *
 * 포인트 증감은 setter가 아니라 아래 메서드로만 하도록 했다.
 * 잔액 검사와 차감이 한 곳에 있어야 "확인 따로, 차감 따로" 하다가
 * 검사를 빠뜨리는 실수를 막을 수 있다.
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @Column(length = 50)
    private String customerId;

    @Column(nullable = false, length = 100)
    private String customerPassword;

    @Column(nullable = false)
    private Double customerPoint;

    /**
     * 동시에 같은 고객의 포인트를 수정하면 나중 트랜잭션이 실패하도록 한다.
     * 이 필드가 없으면 두 주문이 동시에 들어올 때 잔액 검사를 각각 통과해
     * 포인트가 음수가 될 수 있다.
     */
    @Version
    private Long version;

    public Customer(String customerId, String customerPassword, Double customerPoint) {
        this.customerId = customerId;
        this.customerPassword = customerPassword;
        this.customerPoint = customerPoint;
    }

    /** 결제. 잔액이 모자라면 차감하지 않고 예외를 던진다. */
    public void pay(double amount) {
        if (customerPoint < amount) {
            throw new ResponseException(Error.INSUFFICIENT_FUNDS,
                    "포인트가 부족합니다. 필요: %.0f, 보유: %.0f".formatted(amount, customerPoint));
        }
        this.customerPoint -= amount;
    }

    /** 주문 취소 시 환급. */
    public void refund(double amount) {
        this.customerPoint += amount;
    }
}
