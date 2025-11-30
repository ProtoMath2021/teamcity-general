# Stage 1: Build base image with all dependencies pre-cached
FROM maven:3.9-eclipse-temurin-17 AS deps-builder

WORKDIR /teamcity-dsl

# Copy only pom.xml first to cache dependencies
COPY pom.xml .

# Download all dependencies (this layer will be cached)
RUN mvn dependency:go-offline -B \
    && mvn dependency:resolve-plugins -B

# Stage 2: Final image with dependencies baked in
FROM maven:3.9-eclipse-temurin-17

LABEL maintainer="TeamCity DSL Builder"
LABEL description="Pre-built image for TeamCity Kotlin DSL compilation"

WORKDIR /teamcity-dsl

# Copy cached Maven repository from builder stage
COPY --from=deps-builder /root/.m2 /root/.m2

# Copy pom.xml for reference
COPY pom.xml .

# Default command: compile and generate configs
CMD ["mvn", "compile", "teamcity-configs:generate", "-B"]
