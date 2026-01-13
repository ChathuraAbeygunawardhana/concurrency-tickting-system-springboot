# Build Stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copy Maven files first for better layer caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw.cmd .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install wget for health check
RUN apk add --no-cache wget

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the jar file
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]