# 1. Usamos la versión moderna y soportada de Maven con Temurin (Java 17)
FROM maven:3.8.5-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2. Usamos la versión de ejecución de Temurin (Jammy es el sistema operativo base de Ubuntu)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]