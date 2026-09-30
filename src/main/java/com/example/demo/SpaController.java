package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Готовое приложение: сайт (Angular) лежит внутри бэкенда в resources/static.
// Адреса страниц сайта (/login, /services/gas, /employees ...) отдают index.html,
// а дальше страницу показывает сам Angular. Адреса /api/** сюда не попадают.
@Controller
public class SpaController {

    @GetMapping({"/login", "/employees", "/tariffs", "/services/**"})
    public String page() {
        return "forward:/index.html";
    }
}
