# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime

FROM eclipse-temurin:21-jre-ubi10-minimal
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
USER app

ENTRYPOINT ["java", "-jar", "app.jar"]