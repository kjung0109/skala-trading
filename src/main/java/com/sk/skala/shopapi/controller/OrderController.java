package com.sk.skala.shopapi.controller;

import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.dto.OrderRequest;
import com.sk.skala.shopapi.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "주문", description = "지정가 주문 접수·취소 및 체결 조회 API. 로그인이 필요합니다")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "지정가 주문",
            description = """
                    매수/매도 주문을 접수하고 즉시 매칭을 시도합니다.
                    가격 우선 → 시간 우선으로 체결되며, 남은 수량은 호가창에 등록됩니다.
                    매수는 주문 시점에 예수금이, 매도는 보유 수량이 묶입니다.
                    """)
    public Response placeOrder(@Valid @RequestBody OrderRequest request) {
        return orderService.placeOrder(request);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "주문 취소",
            description = "미체결 잔량에 대해 묶어둔 예수금·보유 수량을 돌려줍니다. 이미 체결된 부분은 되돌리지 않습니다")
    public Response cancelOrder(@PathVariable Long orderId) {
        return orderService.cancelOrder(orderId);
    }

    @GetMapping("/me")
    @Operation(summary = "내 주문 목록", description = "미체결·부분체결·전량체결·취소 상태를 모두 포함합니다")
    public Response getMyOrders(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "count", defaultValue = "20") int count) {
        return orderService.getMyOrders(offset, count);
    }

    @GetMapping("/me/trades")
    @Operation(summary = "내 체결 내역")
    public Response getMyTrades(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "count", defaultValue = "20") int count) {
        return orderService.getMyTrades(offset, count);
    }
}
