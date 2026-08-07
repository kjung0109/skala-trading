# ── 빌드 단계 ───────────────────────────────────────────────
# JDK는 컴파일에만 필요하다. 최종 이미지에 남기지 않으려고 단계를 나눈다.
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# 의존성 정의만 먼저 복사해 받아둔다.
# 소스만 고쳤을 때 이 레이어가 캐시에 남아 재빌드가 훨씬 빨라진다.
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
# 이미지 빌드 중에는 테스트를 돌리지 않는다.
# 테스트는 로컬에서 이미 검증했고, 여기서 돌리면 빌드 시간만 길어진다.
RUN ./gradlew bootJar --no-daemon -x test

# ── 실행 단계 ───────────────────────────────────────────────
FROM eclipse-temurin:21-jre

# 헬스체크에만 쓰려고 curl을 넣는다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# root로 돌리지 않는다. 컨테이너가 뚫려도 할 수 있는 일을 줄인다.
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser

WORKDIR /app
COPY --from=build --chown=appuser:appuser /app/build/libs/*.jar app.jar

USER appuser
EXPOSE 8080

# 컨테이너가 떠 있는 것과 요청을 받을 수 있는 것은 다르다.
# Actuator로 기동 완료를 확인해야 compose의 depends_on(service_healthy)이 의미를 가진다.
HEALTHCHECK --interval=10s --timeout=3s --start-period=45s --retries=6 \
  CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
