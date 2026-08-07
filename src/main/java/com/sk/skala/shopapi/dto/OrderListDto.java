package com.sk.skala.shopapi.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** 고객 상세 조회 응답. 고객 정보와 주문한 상품 목록을 함께 담는다. */
@Getter
@Builder
@NoArgsConstructor
@lombok.AllArgsConstructor
public class OrderListDto {

    private String customerId;
    private Double customerPoint;
    private List<OrderItemDto> products;
}
