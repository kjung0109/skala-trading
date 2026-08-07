package com.sk.skala.shopapi.dto;

import com.sk.skala.shopapi.domain.OrderItem;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@lombok.AllArgsConstructor
public class OrderItemDto {

    private Long productId;
    private String productName;
    private Double productPrice;
    private Integer quantity;

    public static OrderItemDto from(OrderItem item) {
        return OrderItemDto.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getProductName())
                .productPrice(item.getProduct().getProductPrice())
                .quantity(item.getQuantity())
                .build();
    }
}
