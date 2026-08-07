package com.sk.skala.trading.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.skala.trading.account.Account;
import com.sk.skala.trading.account.AccountRepository;
import com.sk.skala.trading.audit.OrderAuditLog;
import com.sk.skala.trading.audit.OrderAuditLogRepository;
import com.sk.skala.trading.stock.Stock;
import com.sk.skala.trading.stock.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP 계층까지 포함한 통합 검증.
 *
 * 인증(JWT) · 입력 검증 · 예외 처리 · 취소 환불 · AOP 감사 로그가
 * 실제 요청 경로에서 함께 동작하는지 확인한다.
 *
 * 감사 로그는 REQUIRES_NEW로 커밋되므로 테스트 트랜잭션으로 되돌릴 수 없다.
 * 그래서 이 클래스에는 @Transactional을 붙이지 않고, 검증도 "방금 남은 마지막 기록"만 본다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StockRepository stockRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private OrderAuditLogRepository auditLogRepository;

    private static final String ACCOUNT = "trader01";

    private String token;
    private Stock stock;

    @BeforeEach
    void setUp() throws Exception {
        stock = stockRepository.findAll().get(0);
        token = login(ACCOUNT, "pw1234");
    }

    @Test
    @DisplayName("로그인하면 액세스 토큰을 받는다")
    void login_returnsToken() {
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("토큰 없이 주문하면 401")
    void order_withoutToken() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(stock.getId(), "BUY", "LIMIT", 1000, 1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.result").value("fail"))
                .andExpect(jsonPath("$.error.code").value("NOT_AUTHENTICATED"));
    }

    @Test
    @DisplayName("수량이 0이면 400과 함께 어떤 항목이 잘못됐는지 알려준다")
    void order_withInvalidQuantity() throws Exception {
        mockMvc.perform(authorized(post("/api/orders"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(stock.getId(), "BUY", "LIMIT", 1000, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
                // 어떤 필드가 왜 틀렸는지는 body에 항목별로 담아 내려준다.
                .andExpect(jsonPath("$.body[0]").value(org.hamcrest.Matchers.containsString("quantity")));
    }

    @Test
    @DisplayName("없는 종목에 주문하면 404")
    void order_withUnknownStock() throws Exception {
        mockMvc.perform(authorized(post("/api/orders"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(999_999L, "BUY", "LIMIT", 1000, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DATA_NOT_FOUND"));
    }

    @Test
    @DisplayName("매핑되지 않은 경로는 500이 아니라 404가 나온다")
    void unknownPath_returns404() throws Exception {
        // @ExceptionHandler(Exception.class)만 두면 이 경우까지 500으로 삼켜진다.
        mockMvc.perform(get("/api/no-such-endpoint"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("주문을 취소하면 묶여 있던 예수금이 돌아온다")
    void cancelOrder_refundsReservedFunds() throws Exception {
        long price = 1_000L;   // 체결되지 않도록 시세와 동떨어진 가격으로 건다
        long before = balance();

        MvcResult placed = mockMvc.perform(authorized(post("/api/orders"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(stock.getId(), "BUY", "LIMIT", price, 10)))
                .andExpect(status().isOk())
                .andReturn();

        long orderId = json(placed).path("body").path("order").path("orderId").asLong();
        assertThat(balance()).isEqualTo(before - price * 10);

        mockMvc.perform(authorized(post("/api/orders/" + orderId + "/cancel")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.status").value("CANCELLED"));

        assertThat(balance()).isEqualTo(before);
    }

    @Test
    @DisplayName("AOP — 성공한 주문이 감사 로그에 남는다")
    void auditLog_onSuccess() throws Exception {
        mockMvc.perform(authorized(post("/api/orders"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(stock.getId(), "BUY", "LIMIT", 1_000L, 1)))
                .andExpect(status().isOk());

        OrderAuditLog log = latestLog();
        assertThat(log.getAction()).isEqualTo(OrderAuditLog.Action.PLACE);
        assertThat(log.isSuccess()).isTrue();
        assertThat(log.getDetail()).contains(stock.getCode()).contains("BUY");
        assertThat(log.getElapsedMs()).isNotNull();
    }

    @Test
    @DisplayName("AOP — 거절된 주문도 사유와 함께 남는다 (REQUIRES_NEW)")
    void auditLog_survivesRollback() throws Exception {
        long before = balance();

        mockMvc.perform(authorized(post("/api/orders"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(stock.getId(), "BUY", "LIMIT", stock.getCurrentPrice(), 1_000_000)))
                .andExpect(status().isBadRequest());

        // 주문은 없던 일이 되어야 한다.
        assertThat(balance()).isEqualTo(before);

        // 그러나 시도한 기록은 남아야 한다. 같은 트랜잭션이었다면 함께 롤백됐을 것이다.
        OrderAuditLog log = latestLog();
        assertThat(log.isSuccess()).isFalse();
        assertThat(log.getMessage()).contains("예수금");
    }

    @Test
    @DisplayName("감사 로그는 API로도 조회할 수 있다")
    void auditLog_isQueryable() throws Exception {
        mockMvc.perform(authorized(post("/api/orders"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(stock.getId(), "BUY", "LIMIT", 1_000L, 1)))
                .andExpect(status().isOk());

        mockMvc.perform(authorized(get("/api/audit/me?offset=0&count=10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.list[0].action").value("PLACE"))
                .andExpect(jsonPath("$.body.list[0].detail").exists());
    }

    // ── helpers ─────────────────────────────────────────────

    private String login(String accountId, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"%s","password":"%s"}
                                """.formatted(accountId, password)))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).path("body").path("accessToken").asText();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authorized(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String orderJson(Long stockId, String side, String type, long price, long quantity) {
        return """
                {"stockId":%d,"side":"%s","type":"%s","price":%d,"quantity":%d}
                """.formatted(stockId, side, type, price, quantity);
    }

    private long balance() {
        return accountRepository.findById(ACCOUNT).map(Account::getBalance).orElseThrow();
    }

    private OrderAuditLog latestLog() {
        return auditLogRepository
                .findByAccountIdOrderByIdDesc(ACCOUNT, PageRequest.of(0, 1))
                .getContent()
                .get(0);
    }
}
