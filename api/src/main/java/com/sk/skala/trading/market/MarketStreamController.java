package com.sk.skala.trading.market;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Tag(name = "실시간 시세", description = "체결·호가 변경을 실시간으로 전송하는 스트림")
public class MarketStreamController {

    private final MarketEventBroadcaster broadcaster;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "실시간 시세 스트림 구독",
            description = """
                    체결(trade)과 호가 변경(order_book) 이벤트를 SSE로 전송합니다.
                    브라우저에서 EventSource로 구독하면 새로고침 없이 호가창이 갱신됩니다.

                    Swagger UI에서는 스트림이 계속 열려 있어 응답이 끝나지 않습니다.
                    확인하려면 터미널에서 curl -N http://localhost:8080/api/market/stream 을 쓰세요.
                    """)
    public SseEmitter stream() {
        return broadcaster.subscribe();
    }

    @GetMapping("/subscribers")
    @Operation(summary = "현재 구독자 수", description = "실시간 스트림에 접속 중인 화면 수")
    public Map<String, Integer> subscribers() {
        return Map.of("subscribers", broadcaster.subscriberCount());
    }
}
