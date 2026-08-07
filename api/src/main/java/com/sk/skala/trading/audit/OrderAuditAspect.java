package com.sk.skala.trading.audit;

import com.sk.skala.trading.common.Response;
import com.sk.skala.trading.common.SessionHandler;
import com.sk.skala.trading.order.dto.OrderRequest;
import com.sk.skala.trading.order.dto.OrderResultDto;
import com.sk.skala.trading.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 주문·취소를 가로채 감사 로그를 남긴다.
 *
 * 왜 AOP인가: 성공이든 실패든 모든 주문을 빠짐없이 기록해야 하는데,
 * 이를 OrderService 안에 넣으면 매칭 로직 곳곳에 try/catch와 기록 호출이 섞인다.
 * 기록은 주문의 본질이 아니라 공통 관심사이므로 밖으로 뺐다.
 *
 * @Order(1)이 중요하다. 기본값이면 @Transactional 프록시 안쪽에서 실행되어
 * proceed() 직후에도 아직 커밋 전이고, 예외가 트랜잭션 경계를 넘기 전에 잡혀
 * 롤백 여부를 알 수 없다. 1을 줘서 트랜잭션보다 바깥에 세운다.
 */
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class OrderAuditAspect {

    private final OrderAuditService auditService;
    private final SessionHandler sessionHandler;
    private final StockRepository stockRepository;

    /**
     * 감사 대상에서 제외할 계좌.
     *
     * 감사 로그의 목적은 사용자 행위 추적이다. 시장을 굴리는 계좌까지 기록하면
     * 사람이 낸 주문이 묻히고 테이블만 계속 커진다.
     * 처음엔 "bot"으로 시작하는 계좌만 걸렀는데, 호가 공급 계좌(market01)가 빠져
     * 초당 두어 건씩 쌓이고 있었다.
     */
    @Value("${app.audit.exclude-accounts:market01,bot01,bot02}")
    private Set<String> excludedAccounts;

    @Around("execution(* com.sk.skala.trading.order.OrderService.placeOrder(..))")
    public Object auditPlace(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        // placeOrder(request) / placeOrder(accountId, request) 두 형태를 모두 받는다.
        String accountId = args[0] instanceof String s ? s : null;
        OrderRequest request = (OrderRequest) args[args.length - 1];

        return audit(joinPoint, accountId, OrderAuditLog.Action.PLACE, describe(request));
    }

    @Around("execution(* com.sk.skala.trading.order.OrderService.cancelOrder(..))")
    public Object auditCancel(ProceedingJoinPoint joinPoint) throws Throwable {
        return audit(joinPoint, null, OrderAuditLog.Action.CANCEL,
                "주문 #" + joinPoint.getArgs()[0] + " 취소");
    }

    private Object audit(ProceedingJoinPoint joinPoint, String accountId,
                         OrderAuditLog.Action action, String detail) throws Throwable {
        long startedAt = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            record(accountId, action, detail, true, summarize(result), startedAt);
            return result;
        } catch (Throwable e) {
            record(accountId, action, detail, false, e.getMessage(), startedAt);
            throw e;
        }
    }

    private void record(String accountId, OrderAuditLog.Action action, String detail,
                        boolean success, String message, long startedAt) {
        String who = accountId != null ? accountId : currentAccountId();
        if (who == null || excludedAccounts.contains(who)) {
            return;
        }
        auditService.record(who, action, detail, success, message, System.currentTimeMillis() - startedAt);
    }

    /** 인증되지 않은 호출이면 예외 대신 null을 돌려 감사 로그만 건너뛴다. */
    private String currentAccountId() {
        try {
            return sessionHandler.getCurrentAccountId();
        } catch (Exception e) {
            return null;
        }
    }

    private String describe(OrderRequest request) {
        String code = stockRepository.findById(request.getStockId())
                .map(stock -> stock.getCode() + " " + stock.getName())
                .orElse("종목#" + request.getStockId());

        return "%s %s %s %s x %d주".formatted(
                code,
                request.getSide(),
                request.getType(),
                request.isMarketOrder() || request.getPrice() == null
                        ? "-"
                        : String.format("%,d원", request.getPrice()),
                request.getQuantity());
    }

    /** 체결 결과 문구(전량 체결·일부 체결·미체결)를 그대로 기록한다. */
    private String summarize(Object result) {
        if (result instanceof Response response && response.getBody() instanceof OrderResultDto dto) {
            return dto.getMessage();
        }
        return "성공";
    }
}
