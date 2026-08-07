package com.sk.skala.shopapi.domain;

import com.sk.skala.shopapi.common.Error;
import com.sk.skala.shopapi.exception.ResponseException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객이 주문한 상품. Customer와 Product를 잇는 매핑 엔티티다.
 *
 * 연관관계는 모두 LAZY로 둔다. 목록 조회에서 필요할 때만 fetch join으로 함께 읽어
 * N+1 문제를 피한다.
 */
@Entity
@Table(name = "order_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    public OrderItem(Customer customer, Product product, Integer quantity) {
        this.customer = customer;
        this.product = product;
        this.quantity = quantity;
    }

    public void addQuantity(int amount) {
        this.quantity += amount;
    }

    /** 취소 수량만큼 줄인다. 보유 수량보다 많이 취소하려 하면 예외. */
    public void reduceQuantity(int amount) {
        if (this.quantity < amount) {
            throw new ResponseException(Error.INSUFFICIENT_QUANTITY,
                    "보유 수량이 부족합니다. 보유: %d, 취소 요청: %d".formatted(this.quantity, amount));
        }
        this.quantity -= amount;
    }

    public boolean isEmpty() {
        return this.quantity <= 0;
    }
}
