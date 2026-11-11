# -------- STAGE 1: Build the application --------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom first (for dependency caching)
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source code
COPY src ./src

# Build the jar
RUN mvn -B clean package -DskipTests

# -------- STAGE 2: Run the application --------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
