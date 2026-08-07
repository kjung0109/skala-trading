package com.sk.skala.trading.audit;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderAuditLogDto {

    private Long logId;
    private String action;
    private String detail;
    private boolean success;
    private String message;
    private Long elapsedMs;
    private LocalDateTime createdAt;

    public static OrderAuditLogDto from(OrderAuditLog log) {
        return OrderAuditLogDto.builder()
                .logId(log.getId())
                .action(log.getAction().name())
                .detail(log.getDetail())
                .success(log.isSuccess())
                .message(log.getMessage())
                .elapsedMs(log.getElapsedMs())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
