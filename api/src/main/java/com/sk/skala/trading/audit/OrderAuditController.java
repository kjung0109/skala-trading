package com.sk.skala.trading.audit;

import com.sk.skala.trading.common.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "감사 로그", description = "AOP로 수집한 주문 처리 기록")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class OrderAuditController {

    private final OrderAuditService auditService;

    @GetMapping("/me")
    @Operation(summary = "내 주문 처리 기록 조회",
            description = "성공한 주문뿐 아니라 거절된 주문도 사유와 함께 남는다. 처리 소요 시간(ms)도 함께 기록된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요함")
    })
    public Response getMyLogs(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int count) {
        return auditService.getMyLogs(offset, count);
    }
}
