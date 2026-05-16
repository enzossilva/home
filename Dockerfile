# Stage 1: Build React
FROM node:20-alpine AS frontend
WORKDIR /app/react-frontend
COPY react-frontend/package*.json ./
RUN npm install
COPY react-frontend/ ./
RUN npm run build

# Stage 2: Build Spring Boot
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY demo/ ./demo/
COPY --from=frontend /app/react-frontend/dist ./demo/src/main/resources/static/
WORKDIR /app/demo
RUN ./mvnw clean package -DskipTests

# Stage 3: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend /app/demo/target/demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
