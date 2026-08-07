package com.sk.skala.trading.domain;

import com.sk.skala.trading.common.Error;
import com.sk.skala.trading.exception.ResponseException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 거래 계좌. 식별자는 사용자가 정한 계좌 ID다.
 *
 * 예수금 증감은 setter가 아니라 아래 메서드로만 한다.
 * 잔액 검사와 차감이 한 곳에 있어야 "확인 따로, 차감 따로" 하다가
 * 검사를 빠뜨리는 실수를 막을 수 있다.
 */
@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @Column(length = 50)
    private String accountId;

    @Column(nullable = false, length = 100)
    private String password;

    /** 예수금 */
    @Column(nullable = false)
    private Long balance;

    /**
     * 동시에 같은 계좌의 예수금을 수정하면 나중 트랜잭션이 실패하도록 한다.
     * 이 필드가 없으면 두 주문이 동시에 들어올 때 잔액 검사를 각각 통과해
     * 예수금이 음수가 될 수 있다.
     */
    @Version
    private Long version;

    public Account(String accountId, String password, Long balance) {
        this.accountId = accountId;
        this.password = password;
        this.balance = balance;
    }

    /** 매수 주문 시 주문금액만큼 예수금을 묶는다(증거금). 부족하면 예외. */
    public void withdraw(long amount) {
        if (balance < amount) {
            throw new ResponseException(Error.INSUFFICIENT_FUNDS,
                    "예수금이 부족합니다. 필요: %,d원, 보유: %,d원".formatted(amount, balance));
        }
        this.balance -= amount;
    }

    public void deposit(long amount) {
        this.balance += amount;
    }
}
