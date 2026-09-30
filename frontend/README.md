# Zetta Billing — сайт на React

Сайт для сотрудников муниципальной инспекции. Бэкенд — Spring Boot из этого же репозитория
(папка `src/`), сайт обращается к нему по адресам `/api/...`.

Сделан на **React 19 + TypeScript + Vite**, страницы переключает **React Router**.

## Запуск при разработке

1. Запустите бэкенд (класс `ZettaBilling` в IntelliJ или `./mvnw spring-boot:run`) — он работает на http://localhost:8080.
2. В папке `frontend`:
   ```
   npm install
   npm run dev
   ```
3. Откройте http://localhost:5173. Запросы `/api` сами уходят на бэкенд (см. `vite.config.ts`).
   Изменения в коде сразу видны в браузере.

Вход: `director` / `director123` или `inspector` / `inspector123` (тестовые данные).

## Сборка

```
npm run build
```

Готовый сайт появляется в `frontend/dist`. Dockerfile и сборка Windows-приложения
(`.github/workflows/build-app.yml`) делают это сами и кладут сайт внутрь бэкенда
(`src/main/resources/static`), так что сайт и API отдаёт один сервер.

Чтобы собрать JAR с сайтом вручную:
```
cd frontend && npm run build && cd ..
mkdir -p src/main/resources/static && cp -r frontend/dist/* src/main/resources/static/
./mvnw -DskipTests package
```

## Что где лежит

| Файл / папка | Что это |
|---|---|
| `src/App.tsx` | шапка, меню слева и список страниц (адреса) |
| `src/api.ts` | все запросы к бэкенду и понятные тексты ошибок |
| `src/auth/` | вход/выход, проверка доступа к страницам (директор / инспектор) |
| `src/TariffContext.tsx` | действующие тарифы (нужны нескольким страницам) |
| `src/services.ts` | список коммунальных услуг в меню |
| `src/pages/` | страницы: вход, абоненты, абонент, сотрудники, тарифы |
| `src/components/` | всплывающие окна: абонент, статус оплаты, сотрудник, время работы |
| `src/styles.css` | все стили |
| `public/` | логотип и значок сайта |
