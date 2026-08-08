package com.sk.skala.trading.stock;

import com.sk.skala.trading.common.Response;
import com.sk.skala.trading.stock.Stock;
import com.sk.skala.trading.order.OrderService;
import com.sk.skala.trading.stock.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Tag(name = "종목", description = "종목 시세 조회 및 관리 API")
public class StockController {

    private final StockService stockService;
    private final OrderService orderService;

    @GetMapping("/list")
    @Operation(summary = "전체 종목 목록", description = "현재가·전일대비·등락률을 함께 반환합니다")
    public Response getAllStocks(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "count", defaultValue = "20") int count) {
        return stockService.getAllStocks(offset, count);
    }

    @GetMapping("/{id}")
    @Operation(summary = "종목 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 종목")
    })
    public Response getStockById(@PathVariable Long id) {
        return stockService.getStockById(id);
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "종목 코드로 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 종목코드")
    })
    public Response getStockByCode(@PathVariable String code) {
        return stockService.getStockByCode(code);
    }

    @GetMapping("/{id}/orderbook")
    @Operation(summary = "호가창 조회",
            description = "가격대별 매도·매수 미체결 잔량을 집계해 반환합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. 매도·매수 호가를 가격대별 잔량으로 반환"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 종목")
    })
    public Response getOrderBook(@PathVariable Long id) {
        return orderService.getOrderBook(id);
    }

    @GetMapping("/{id}/chart")
    @Operation(summary = "가격 추이 (캔들)",
            description = "체결을 지정한 시간 구간으로 접어 시가·고가·저가·종가·거래량을 만든다. 차트용이다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 종목")
    })
    public Response getCandles(
            @PathVariable Long id,
            @Parameter(description = "캔들 하나가 담는 시간(초)")
            @RequestParam(defaultValue = "5") int interval) {
        return orderService.getCandles(id, interval);
    }

    @GetMapping("/{id}/trades")
    @Operation(summary = "종목별 체결 내역", description = "최근 체결 순으로 반환합니다")
    public Response getStockTrades(@PathVariable Long id) {
        return orderService.getStockTrades(id);
    }

    @PostMapping
    @Operation(summary = "종목 등록")
    public Response createStock(@RequestBody Stock stock) {
        return stockService.createStock(stock);
    }

    @PutMapping("/{id}")
    @Operation(summary = "종목 수정")
    public Response updateStock(@PathVariable Long id, @RequestBody Stock stock) {
        return stockService.updateStock(id, stock);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "종목 삭제")
    public Response deleteStock(@PathVariable Long id) {
        return stockService.deleteStock(id);
    }
}
