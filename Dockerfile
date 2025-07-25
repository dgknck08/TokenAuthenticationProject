# Build aşaması
FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw clean package -DskipTests

# Çalıştırma aşaması
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/ecommerce-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
