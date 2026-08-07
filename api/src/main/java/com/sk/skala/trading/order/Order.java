package com.sk.skala.trading.order;

import com.sk.skala.trading.common.Error;
import com.sk.skala.trading.exception.ResponseException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import com.sk.skala.trading.account.Account;
import com.sk.skala.trading.stock.Stock;

/**
 * 지정가 주문.
 *
 * quantity는 주문 수량, remainingQuantity는 아직 체결되지 않은 잔량이다.
 * 부분 체결을 지원하려면 둘을 나눠 관리해야 한다.
 */
@Entity
@Table(name = "orders", indexes = {
        // 호가창 조회와 매칭이 이 조건으로 검색하므로 인덱스를 건다.
        @Index(name = "idx_order_book", columnList = "stock_id, side, status, price, createdAt")
})
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderType type;

    /** 지정가 */
    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Long quantity;

    /** 미체결 잔량 */
    @Column(nullable = false)
    private Long remainingQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Order(Account account, Stock stock, OrderSide side, OrderType type, Long price, Long quantity) {
        this.account = account;
        this.stock = stock;
        this.side = side;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.remainingQuantity = quantity;
        this.status = OrderStatus.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    /** 체결된 수량만큼 잔량을 줄이고 상태를 갱신한다. */
    public void fill(long filledQuantity) {
        if (filledQuantity > remainingQuantity) {
            throw new IllegalStateException("잔량보다 많은 수량을 체결할 수 없습니다");
        }
        this.remainingQuantity -= filledQuantity;
        this.status = (remainingQuantity == 0) ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
    }

    public void cancel() {
        if (status == OrderStatus.FILLED) {
            throw new ResponseException(Error.ORDER_ALREADY_FILLED, "이미 전량 체결된 주문입니다");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new ResponseException(Error.ORDER_ALREADY_CANCELLED, "이미 취소된 주문입니다");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public long filledQuantity() {
        return quantity - remainingQuantity;
    }

    /**
     * 시장가 주문의 미체결 잔량을 정리한다.
     * 시장가는 호가창에 남기지 않는 것이 원칙이므로, 채우지 못한 만큼은 취소 처리한다.
     */
    public void expireRemaining() {
        if (remainingQuantity > 0) {
            // 잔량이 남으면 EXPIRED로 둔다.
            // PARTIALLY_FILLED로 두면 호가창 조회·매칭 대상에 계속 잡혀
            // 시장가 주문이 지정가처럼 대기하게 된다.
            this.status = OrderStatus.EXPIRED;
        }
    }
}
