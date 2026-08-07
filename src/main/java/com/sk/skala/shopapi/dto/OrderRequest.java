package com.sk.skala.shopapi.dto;

import com.sk.skala.shopapi.domain.OrderSide;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 지정가 주문 요청. 계좌는 토큰에서 식별하므로 본문에 담지 않는다. */
@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {

    @NotNull(message = "종목 ID는 필수입니다")
    private Long stockId;

    @NotNull(message = "매수/매도 구분은 필수입니다")
    private OrderSide side;

    @NotNull(message = "주문 가격은 필수입니다")
    @Min(value = 1, message = "주문 가격은 1원 이상이어야 합니다")
    private Long price;

    @NotNull(message = "주문 수량은 필수입니다")
    @Min(value = 1, message = "주문 수량은 1주 이상이어야 합니다")
    private Long quantity;
}
