package com.sk.skala.trading.account;

import com.sk.skala.trading.common.Response;
import com.sk.skala.trading.account.dto.AccountSession;
import com.sk.skala.trading.account.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "계좌", description = "계좌 개설·로그인·잔고 조회 API")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "계좌 개설", description = "개설 시 초기 예수금이 지급됩니다. 비밀번호는 해시로 저장됩니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "개설 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 계좌 아이디")
    })
    public Response createAccount(@Valid @RequestBody AccountSession request) {
        return accountService.createAccount(request);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "성공 시 JWT를 발급하고 쿠키(bff-access)로도 내려줍니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공. 액세스 토큰 발급"),
            @ApiResponse(responseCode = "400", description = "입력값 오류"),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호가 올바르지 않음")
    })
    public Response login(@Valid @RequestBody AccountSession request) {
        return accountService.login(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public Response logout() {
        return accountService.logout();
    }

    @GetMapping("/me")
    @Operation(summary = "내 계좌 현황",
            description = "예수금·보유종목·평가손익·총자산을 한 번에 조회합니다. 로그인이 필요합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요함")
    })
    public Response getMySummary() {
        return accountService.getMySummary();
    }

    @GetMapping("/list")
    @Operation(summary = "전체 계좌 목록", description = "offset/count로 페이지 단위 조회합니다")
    public Response getAllAccounts(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "count", defaultValue = "10") int count) {
        return accountService.getAllAccounts(offset, count);
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "계좌 현황 조회", description = "지정한 계좌의 보유 종목과 평가손익을 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 계좌")
    })
    public Response getSummary(@PathVariable String accountId) {
        return accountService.getSummary(accountId);
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "계좌 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 계좌")
    })
    public Response deleteAccount(@PathVariable String accountId) {
        return accountService.deleteAccount(accountId);
    }
}
