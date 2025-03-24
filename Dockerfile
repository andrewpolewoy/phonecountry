FROM gradle:8.0.2-jdk17 AS builder
WORKDIR /app

# Copy Gradle files and resolve dependencies
COPY build.gradle settings.gradle gradle.properties gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

# Copy source code and build
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# Second stage: Run the application
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose port and run
EXPOSE 8088
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]