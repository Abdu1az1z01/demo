# Сборка Zetta Billing для сервера в интернете (Render, Railway, любой VPS с Docker).
# Хостинг сам выполняет этот файл: собирает сайт, собирает бэкенд и запускает всё вместе.
#
# Нужные переменные окружения на хостинге:
#   DATABASE_URL             — адрес базы PostgreSQL (postgresql://логин:пароль@хост/база)
#   ZETTA_DIRECTOR_PASSWORD  — пароль директора (логин director)
#   ZETTA_DEMO_DATA=true     — (необязательно) заполнить тестовыми абонентами

# ---------- 1. Сайт (Angular) из репозитория communal-frontend ----------
FROM node:24-alpine AS frontend
ARG FRONTEND_BRANCH=Frotend
# Скачиваем архив ветки фронтенда с GitHub. Если во фронтенде появился новый коммит,
# архив меняется и сайт собирается заново (а не берётся из кэша прошлой сборки).
ADD https://github.com/Abdu1az1z01/communal-frontend/archive/refs/heads/${FRONTEND_BRANCH}.tar.gz /tmp/frontend.tar.gz
RUN mkdir /frontend && tar -xzf /tmp/frontend.tar.gz -C /frontend --strip-components=1
WORKDIR /frontend
RUN npm ci && npx ng build

# ---------- 2. Бэкенд (Spring Boot) с сайтом внутри ----------
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sh mvnw -B -q dependency:go-offline
COPY src src
COPY --from=frontend /frontend/dist/communal-frontend/browser/ src/main/resources/static/
RUN sh mvnw -B -q -DskipTests package && cp target/*.jar /app/zetta-billing.jar

# ---------- 3. Запуск ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /app/zetta-billing.jar zetta-billing.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
CMD ["java", "-XX:MaxRAMPercentage=75", "-jar", "zetta-billing.jar"]
