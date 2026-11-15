# Etapa de compilación
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Caché de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compilar aplicación
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:17-jre-alpine

# Adaptador AWS Lambda
COPY --from=public.ecr.aws/awsguru/aws-lambda-adapter:0.8.4 /lambda-adapter /opt/extensions/lambda-adapter

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup && mkdir -p /app/logs && chown -R appuser:appgroup /app
USER appuser

COPY --chown=appuser:appgroup --from=builder /app/target/*.jar app.jar

EXPOSE 8081

ENV SERVER_PORT=8081 \
    PORT=8081 \
    AWS_LWA_PORT=8081 \
    AWS_LWA_READINESS_CHECK_PATH=/actuator/health \
    AWS_LWA_ASYNC_INIT=true \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
