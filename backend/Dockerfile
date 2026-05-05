# Stage 1: Build
FROM gradle:8.5-jdk21-alpine AS build

WORKDIR /app

# Копируем gradle wrapper и конфигурацию
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .

# Копируем все модули
COPY common common
COPY auth-service auth-service
COPY user-service user-service
COPY post-service post-service
COPY chat-service chat-service
COPY recm-service recm-service
COPY app app

# Даем права на выполнение gradlew
RUN chmod +x ./gradlew

# Собираем приложение
RUN ./gradlew :app:bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Создаем пользователя
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копируем jar
COPY --from=build /app/app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]