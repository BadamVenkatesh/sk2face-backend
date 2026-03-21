# ---- Stage 1: Cache Dependencies ----
FROM maven:3.9.6-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn dependency:go-offline -B -q

# ---- Stage 2: Build ----
FROM deps AS builder
COPY src ./src
RUN mvn clean package -DskipTests -q

# ---- Stage 3: Run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]