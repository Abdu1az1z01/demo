# Сборка Zetta Billing для сервера в интернете (Render, Railway, любой VPS с Docker).
# Хостинг сам выполняет этот файл: собирает сайт, собирает бэкенд и запускает всё вместе.
#
# Нужные переменные окружения на хостинге:
#   DATABASE_URL             — адрес базы PostgreSQL (postgresql://логин:пароль@хост/база)
#   ZETTA_DIRECTOR_PASSWORD  — пароль директора (логин director)
#   ZETTA_DEMO_DATA=true     — (необязательно) заполнить тестовыми абонентами

# ---------- 1. Сайт (React) из папки frontend этого репозитория ----------
FROM node:24-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---------- 2. Бэкенд (Spring Boot) с сайтом внутри ----------
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sh mvnw -B -q dependency:go-offline
COPY src src
COPY --from=frontend /frontend/dist/ src/main/resources/static/
RUN sh mvnw -B -q -DskipTests package && cp target/*.jar /app/zetta-billing.jar

# ---------- 3. Запуск ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /app/zetta-billing.jar zetta-billing.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
CMD ["java", "-XX:MaxRAMPercentage=75", "-jar", "zetta-billing.jar"]
