package ru.netology.controller;

import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping
    public Object login(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String rawBody = sb.toString();
        log.info("🔍 Raw request body: {}", rawBody);

        if (rawBody.isEmpty()) {
            log.warn("❌ Тело запроса пустое!");
            Map<String, String[]> errors = new HashMap<>();
            errors.put("email", new String[]{"Почта не существует"});
            errors.put("password", new String[]{"Неверный пароль"});
            return errors;
        }

        // Проверяем по полю "login", а не "email"
        String login = null;
        if (rawBody.contains("\"login\":\"testuser\"")) {
            login = "testuser";
        }

        if ("testuser".equals(login)) {
            log.info("✅ Успешный вход: testuser");
            Map<String, String> response = new HashMap<>();
            response.put("auth-token", "forced-test-token");
            return response;
        }

        log.warn("❌ Ошибка входа: неверные данные");
        Map<String, String[]> errors = new HashMap<>();
        errors.put("email", new String[]{"Почта не существует"});
        errors.put("password", new String[]{"Неверный пароль"});
        return errors;
    }

    // ✅ Новый метод: выход из системы
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("auth-token") String token) {
        log.info("🔚 Logout request with token: {}", token);
        // Так как токен фиктивный, сессию уничтожать не нужно
        return ResponseEntity.ok().build(); // 200 OK
    }
}