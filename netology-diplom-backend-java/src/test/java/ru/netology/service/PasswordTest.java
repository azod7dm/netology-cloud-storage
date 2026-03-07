package ru.netology.service;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

public class PasswordTest {

    public static void main(String[] args) {
        String password = "password";
        String saltBase64 = "cGFzc3dvcmQ="; // Base64 от строки "password"

        String hash = hashPassword(password, saltBase64);
        System.out.println("Сгенерированный хэш: " + hash);
    }

    private static String hashPassword(String password, String saltBase64) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            char[] passwordChars = password.toCharArray();
            byte[] saltBytes = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(passwordChars, saltBytes, 1000, 256);
            SecretKey key = factory.generateSecret(spec);
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}