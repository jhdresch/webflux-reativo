## Multi-stage Dockerfile: build with Maven, run with lightweight JRE
# Build stage
FROM maven:3.8.8-openjdk-17 AS build
WORKDIR /workspace

# copy only what is needed for dependency resolution first (speeds up rebuilds)
COPY pom.xml mvnw* ./
COPY .mvn .mvn
RUN mvn -B -N -f pom.xml org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline

# copy sources and build
COPY src ./src
RUN mvn -B -f pom.xml clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY --from=build /workspace/target/*.jar /app/app.jar

# Default server port for the runtime image. Can be overridden with -e SERVER_PORT=...
ENV SERVER_PORT=8091
EXPOSE 8091

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${SERVER_PORT} -jar /app/app.jar"]

