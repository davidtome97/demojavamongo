# =====================
# BUILD (Maven)
# =====================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1) Copiamos solo pom para cachear dependencias
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 2) Copiamos código y compilamos
COPY src ./src
RUN mvn -B -q -DskipTests clean package

# =====================
# RUNTIME (Java)
# =====================
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]