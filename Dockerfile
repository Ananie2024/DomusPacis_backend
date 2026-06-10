# Step 1: Build the artifact using Maven and Java 21
        FROM maven:3.9.6-eclipse-temurin-21 AS build
        WORKDIR /app
        COPY . .
        RUN mvn clean package -DskipTests

        # Step 2: Run the compiled jar file
        FROM eclipse-temurin:21-jre-jammy
        WORKDIR /app
        COPY --from=build /app/target/*.jar app.jar

        # Expose port 7860 (Hugging Face strictly requires port 7860)
        EXPOSE 7860
        ENV PORT=7860

        ENTRYPOINT ["java", "-jar", "app.jar"]