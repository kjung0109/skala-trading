package com.sk.skala.trading.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 캔들 하나. 한 시간 구간의 시가·고가·저가·종가와 거래량을 담는다.
 *
 * 체결을 그대로 내려보내지 않고 구간으로 접는 이유는 두 가지다.
 * 초당 수십 건이라 원본은 응답이 너무 커지고, 화면에서도 점이 구분되지 않는다.
 */
@Getter
@Builder
public class CandleDto {

    private LocalDateTime time;
    private Long open;
    private Long high;
    private Long low;
    private Long close;
    private Long volume;
}
