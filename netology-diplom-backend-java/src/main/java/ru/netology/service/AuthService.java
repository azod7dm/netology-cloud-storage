package ru.netology.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.netology.model.User;
import ru.netology.repository.UserRepository;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final Map<String, String> tokenToLogin = new HashMap<>();

    @Autowired
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String login(String email, String password) {
        log.info("Поиск пользователя: {}", email);
        Optional<User> user = userRepository.findByLogin(email);
        if (user.isPresent()) {
            log.info("Пользователь найден: {}", email);
            User u = user.get();
            String hashedPassword = hashPassword(password, u.getSalt());
            log.info("Введённый пароль (хэш): {}", hashedPassword);
            log.info("Хэш из БД: {}", u.getPasswordHash());
            log.info("Сравнение: {}", hashedPassword.equals(u.getPasswordHash()) ? "OK" : "НЕ СОВПАДАЕТ");
            if (hashedPassword.equals(u.getPasswordHash())) {
                String token = generateToken();
                tokenToLogin.put(token, email);
                log.info("Авторизация успешна, токен: {}", token);
                return token;
            } else {
                log.warn("Неверный пароль для пользователя: {}", email);
            }
        } else {
            log.warn("Пользователь не найден: {}", email);
        }
        return null;
    }

    public boolean isTokenValid(String token) {
        return tokenToLogin.containsKey(token);
    }

    public String getLoginByToken(String token) {
        return tokenToLogin.get(token);
    }

    private String generateToken() {
        return java.util.UUID.randomUUID().toString();
    }

    private String hashPassword(String password, String salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            char[] passwordChars = password.toCharArray();
            byte[] saltBytes = Base64.getDecoder().decode(salt); // ← Должно быть!
            PBEKeySpec spec = new PBEKeySpec(passwordChars, saltBytes, 1000, 256);
            SecretKey key = factory.generateSecret(spec);
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}