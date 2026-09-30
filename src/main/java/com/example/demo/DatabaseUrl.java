package com.example.demo;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

// Хостинги (Render, Neon, Railway, Supabase) выдают адрес базы в виде
//   postgresql://пользователь:пароль@хост:порт/база?sslmode=require
// а Spring ждёт jdbc:postgresql://хост:порт/база + отдельно логин и пароль.
// Если переменная окружения DATABASE_URL задана в таком виде — переводим её сами,
// чтобы можно было просто скопировать адрес с сайта хостинга.
// Адрес вида jdbc:postgresql://... используется как есть (через application-prod.properties).
public final class DatabaseUrl {

    private DatabaseUrl() {
    }

    public static void applyFromEnvironment() {
        String url = System.getenv("DATABASE_URL");
        if (url == null || !(url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
            return;
        }
        URI uri = URI.create(url);
        String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
        String query = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";
        System.setProperty("spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + port + uri.getRawPath() + query);

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            String[] parts = userInfo.split(":", 2);
            System.setProperty("spring.datasource.username", decode(parts[0]));
            if (parts.length > 1) {
                System.setProperty("spring.datasource.password", decode(parts[1]));
            }
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
