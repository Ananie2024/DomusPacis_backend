# Step 1: Build the artifact using Maven and Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run the compiled jar file
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create non-root user for container security
RUN useradd -m -u 10001 appuser
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar
USER appuser

# Expose port 7860 (Hugging Face strictly requires port 7860)
EXPOSE 7860
ENV PORT=7860

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]