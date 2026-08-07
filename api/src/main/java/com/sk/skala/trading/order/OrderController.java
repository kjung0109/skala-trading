package com.sk.skala.trading.order;

import com.sk.skala.trading.common.Response;
import com.sk.skala.trading.order.dto.OrderRequest;
import com.sk.skala.trading.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "주문 접수 (지정가 · 시장가)",
            description = """
                    매수/매도 주문을 접수하고 즉시 매칭을 시도합니다.
                    가격 우선 → 시간 우선으로 체결되며, 매수는 주문 시점에 예수금이, 매도는 보유 수량이 묶입니다.
                    지정가(LIMIT)는 남은 수량을 호가창에 등록하고,
                    시장가(MARKET)는 가격을 부르지 않고 호가를 훑어 체결한 뒤 남은 수량을 소멸시킵니다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 접수 성공. 즉시 체결된 수량과 체결 내역을 함께 반환"),
            @ApiResponse(responseCode = "400", description = "입력값 오류 / 예수금·보유 수량 부족 / 체결 가능한 호가 없음"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요함"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 종목")
    })
    public Response placeOrder(@Valid @RequestBody OrderRequest request) {
        return orderService.placeOrder(request);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "주문 취소",
            description = "미체결 잔량에 대해 묶어둔 예수금·보유 수량을 돌려줍니다. 이미 체결된 부분은 되돌리지 않습니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공. 미체결 잔량만큼 예수금·보유 수량을 반환"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요함"),
            @ApiResponse(responseCode = "403", description = "본인의 주문이 아님"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 주문"),
            @ApiResponse(responseCode = "409", description = "이미 전량 체결되었거나 취소된 주문")
    })
    public Response cancelOrder(@PathVariable Long orderId) {
        return orderService.cancelOrder(orderId);
    }

    @GetMapping("/me")
    @Operation(summary = "내 주문 목록", description = "미체결·부분체결·전량체결·취소 상태를 모두 포함합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요함")
    })
    public Response getMyOrders(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "count", defaultValue = "20") int count) {
        return orderService.getMyOrders(offset, count);
    }

    @GetMapping("/me/trades")
    @Operation(summary = "내 체결 내역")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요함")
    })
    public Response getMyTrades(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "count", defaultValue = "20") int count) {
        return orderService.getMyTrades(offset, count);
    }
}
