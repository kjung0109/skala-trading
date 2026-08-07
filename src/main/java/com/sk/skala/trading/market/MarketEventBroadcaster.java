package com.sk.skala.trading.market;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 접속 중인 화면들에 시장 이벤트를 밀어준다 (Server-Sent Events).
 *
 * WebSocket이 아니라 SSE를 쓰는 이유:
 * 시세·호가는 서버에서 화면으로 흐르기만 하면 되는 단방향 데이터다.
 * 양방향이 필요 없는데 WebSocket을 쓰면 핸드셰이크·세션 관리 비용만 늘어난다.
 * SSE는 일반 HTTP 위에서 동작해 프록시 통과도 쉽다.
 *
 * @TransactionalEventListener(AFTER_COMMIT)을 쓰는 이유:
 * 체결이 커밋되기 전에 이벤트를 보내면, 뒤이어 트랜잭션이 롤백됐을 때
 * 화면에는 존재하지 않는 체결이 표시된다. 커밋이 끝난 뒤에만 내보낸다.
 */
@Slf4j
@Component
public class MarketEventBroadcaster {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        // 0이면 무한 대기. 프록시가 끊으면 클라이언트가 자동 재연결한다.
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);

        try {
            // 첫 이벤트를 즉시 보내 연결이 열렸음을 알린다.
            emitter.send(SseEmitter.event().name("connected").data("연결되었습니다"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        log.debug("[SSE] 구독 시작 - 현재 {}명", emitters.size());
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMarketEvent(MarketEvent event) {
        broadcast(event);
    }

    private void broadcast(MarketEvent event) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.type().name().toLowerCase())
                        .data(event));
            } catch (Exception e) {
                // 끊긴 연결은 조용히 정리한다. 한 명이 끊겼다고 다른 구독자에게 영향을 주면 안 된다.
                emitters.remove(emitter);
            }
        }
    }

    public int subscriberCount() {
        return emitters.size();
    }
}
