# 1. 빌드 단계 (Builder Stage)
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .

# gradlew 파일에 실행 권한 부여
RUN chmod +x ./gradlew


RUN gradle clean build -x test

# 2. 실행 단계 (Runtime Stage)
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
# 빌드 단계에서 생성된 JAR 파일을 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]