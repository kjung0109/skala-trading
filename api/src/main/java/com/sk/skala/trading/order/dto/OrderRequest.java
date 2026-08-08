package com.sk.skala.trading.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sk.skala.trading.order.OrderSide;
import com.sk.skala.trading.order.OrderType;
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

    /**
     * 시장가 주문인지 판단하는 내부 헬퍼.
     *
     * @JsonIgnore가 없으면 Jackson이 isXxx()를 boolean 속성으로 보고
     * 요청 스키마에 marketOrder 필드를 만들어 낸다. 입력값이 아닌데
     * Swagger 예시에 나타나 무엇을 넣어야 하는지 헷갈리게 한다.
     */
    @JsonIgnore
    public boolean isMarketOrder() {
        return type == OrderType.MARKET;
    }


    @NotNull(message = "종목 ID는 필수입니다")
    private Long stockId;

    @NotNull(message = "매수/매도 구분은 필수입니다")
    private OrderSide side;

    /** 지정가(LIMIT) / 시장가(MARKET). 생략하면 지정가로 본다. */
    private OrderType type = OrderType.LIMIT;

    /** 지정가 주문에서만 필수. 시장가는 비워 보낸다. */
    @Min(value = 1, message = "주문 가격은 1원 이상이어야 합니다")
    private Long price;

    @NotNull(message = "주문 수량은 필수입니다")
    @Min(value = 1, message = "주문 수량은 1주 이상이어야 합니다")
    private Long quantity;
}
