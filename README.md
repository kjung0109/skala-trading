# SKALA Trading

호가·체결 기반 주식 거래 시스템. 백엔드 최종 실습과제.

외부 증권 API를 사용하지 않습니다. 주문 접수 · 호가 관리 · 매칭 · 체결 · 정산을
직접 구현했으며, 화면의 현재가는 이 시스템 안에서 체결된 가격입니다.

```
skala-trading/
├── api/   Spring Boot 3.3 · Java 21 · JPA · H2      (:8080)
└── fe/    React 19 · Vite · TypeScript · Tailwind 4 (:5173)
```

## 실행

**백엔드**
```bash
cd api
./gradlew bootRun
```

**프론트엔드**
```bash
cd fe
pnpm install
pnpm dev
```

| 주소 | 설명 |
| --- | --- |
| http://localhost:5173 | 거래 화면 |
| http://localhost:8080/swagger-ui.html | API 문서 |
| http://localhost:8080/h2-console | H2 콘솔 (JDBC URL `jdbc:h2:mem:tradingdb`, 사용자 `sa`) |
| http://localhost:8080/actuator/health | 상태 점검 |

데모 계좌: `trader01` ~ `trader03` / 비밀번호 `pw1234`
