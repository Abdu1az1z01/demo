package com.example.demo;

import java.io.IOException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Готовое приложение: когда сервер запустился, открываем сайт в браузере.
// Включается настройкой zetta.open-browser=true (профиль «app»).
@Component
public class BrowserOpener {

    private static final Logger log = LoggerFactory.getLogger(BrowserOpener.class);

    private final boolean enabled;
    private final int port;

    public BrowserOpener(@Value("${zetta.open-browser:false}") boolean enabled,
                         @Value("${server.port:8080}") int port) {
        this.enabled = enabled;
        this.port = port;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        String url = "http://localhost:" + port;
        log.info("Zetta Billing запущен: {}  (чтобы остановить — закройте это окно)", url);
        if (!enabled) {
            return;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String[] command = os.contains("win") ? new String[] {"rundll32", "url.dll,FileProtocolHandler", url}
                : os.contains("mac") ? new String[] {"open", url}
                : new String[] {"xdg-open", url};
        try {
            new ProcessBuilder(command).start();
        } catch (IOException e) {
            log.warn("Не удалось открыть браузер, откройте вручную: {}", url);
        }
    }
}
