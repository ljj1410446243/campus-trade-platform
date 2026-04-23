FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY . .
RUN mvn -f pom.xml -pl common,notice-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/notice-service/target/*.jar /app/app.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
