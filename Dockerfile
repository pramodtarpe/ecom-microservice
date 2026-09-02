FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
RUN --mount=type=cache,id=ecommerce-gradle-cache,target=/root/.gradle,sharing=locked \
    chmod +x gradlew && ./gradlew --version --no-daemon
COPY settings.gradle build.gradle ./
COPY platform-common platform-common
COPY event-contracts event-contracts
COPY auth-service auth-service
COPY product-service product-service
COPY inventory-service inventory-service
COPY order-service order-service
COPY eureka-server eureka-server
COPY api-gateway api-gateway

ARG SERVICE_NAME
RUN --mount=type=cache,id=ecommerce-gradle-cache,target=/root/.gradle,sharing=locked \
    ./gradlew ":${SERVICE_NAME}:bootJar" --no-daemon \
    && cp "${SERVICE_NAME}"/build/libs/*.jar /tmp/application.jar

FROM eclipse-temurin:21-jre-jammy

WORKDIR /application
RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=builder --chown=10001:0 /tmp/application.jar application.jar

USER 10001
ENTRYPOINT ["java", "-jar", "/application/application.jar"]
