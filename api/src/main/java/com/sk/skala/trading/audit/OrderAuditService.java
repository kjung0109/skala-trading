package com.sk.skala.trading.audit;

import com.sk.skala.trading.common.PagedList;
import com.sk.skala.trading.common.Response;
import com.sk.skala.trading.common.SessionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAuditService {

    private final OrderAuditLogRepository auditLogRepository;
    private final SessionHandler sessionHandler;

    /**
     * 감사 로그를 별도 트랜잭션에 저장한다.
     *
     * REQUIRES_NEW가 없으면 주문이 실패해 롤백될 때 기록까지 함께 사라진다.
     * 실패 기록이야말로 남아야 하는 정보이므로 반드시 분리한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String accountId, OrderAuditLog.Action action, String detail,
                       boolean success, String message, long elapsedMs) {
        try {
            auditLogRepository.save(OrderAuditLog.builder()
                    .accountId(accountId)
                    .action(action)
                    .detail(detail)
                    .success(success)
                    .message(abbreviate(message))
                    .elapsedMs(elapsedMs)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            // 로그를 남기지 못한 것이 주문 자체를 실패시켜서는 안 된다.
            log.warn("감사 로그 저장 실패: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Response getMyLogs(int offset, int count) {
        String accountId = sessionHandler.getCurrentAccountId();
        Page<OrderAuditLog> page = auditLogRepository.findByAccountIdOrderByIdDesc(
                accountId, PageRequest.of(offset / Math.max(count, 1), count));
        return Response.success(PagedList.of(page, offset, count, OrderAuditLogDto::from));
    }

    private String abbreviate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 497) + "...";
    }
}
