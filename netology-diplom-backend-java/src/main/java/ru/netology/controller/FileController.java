package ru.netology.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import ru.netology.entity.File;
import ru.netology.repository.FileRepository;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;


@RestController
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class FileController {

    @Autowired
    private FileRepository fileRepository;

    // Хранилище файлов в памяти (для диплома достаточно)
    private final Map<String, byte[]> fileStorage = new java.util.HashMap<>();

    // GET /list?limit=5
    @GetMapping("/list")
    public ResponseEntity<List<File>> getFiles(
            @RequestHeader("auth-token") String token,
            @RequestParam int limit) {

        String username = getUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<File> files = fileRepository.findByOwnerLogin(username);
        return ResponseEntity.ok(files.stream().limit(limit).toList());
    }

    // POST /file + multipart/form-data
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile file) {

        String username = getUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            byte[] bytes = file.getBytes();
            fileStorage.put(filename, bytes);

            File dbFile = new File();
            dbFile.setFilename(filename);
            dbFile.setSize((long) bytes.length);
            dbFile.setOwnerLogin(username); // ← теперь указываем владельца!
            fileRepository.save(dbFile);

            return ResponseEntity.ok("File uploaded");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
        }
    }

    // DELETE /file?filename=name.txt
    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(
            @RequestHeader("auth-token") String token,
            @RequestParam String filename) {

        System.out.println("\n=== DELETE DEBUG ===");
        System.out.println("Received filename to delete: " + filename);
        System.out.println("Auth token: " + token);

        String username = getUsernameFromToken(token);
        if (username == null) {
            System.out.println("❌ Unauthorized: invalid or missing token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        System.out.println("User: " + username);

        File file = fileRepository.findByFilenameAndOwnerLogin(filename, username);
        if (file == null) {
            System.out.println("❌ File not found in DB for user: " + username + ", filename: " + filename);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }

        System.out.println("✅ File found in DB: id=" + file.getId() + ", filename=" + file.getFilename());

        boolean removedFromStorage = fileStorage.remove(filename) != null;
        System.out.println("🗂️  Removed from in-memory storage: " + removedFromStorage);

        try {
            fileRepository.delete(file); // ← безопасное удаление сущности
            System.out.println("✅ File entity deleted from DB");
        } catch (Exception e) {
            System.out.println("❌ Error deleting from DB: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Delete failed");
        }

        System.out.println("✅ DELETE SUCCESS: " + filename + " removed\n");
        return ResponseEntity.ok("File deleted");
    }

    @PutMapping("/file")
    public ResponseEntity<?> renameFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String oldName,
            @RequestBody Map<String, String> request) {

        String username = getUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Сначала ищем по ключу "newname" — как в ТЗ
        String newName = request.get("newname");

        // Если нет — пробуем любое значение, не равное старому имени
        if (newName == null || newName.isEmpty()) {
            newName = request.values().stream()
                    .filter(value -> !value.equals(oldName))
                    .findFirst()
                    .orElse(null);
        }

        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("New filename is required");
        }

        File file = fileRepository.findByFilenameAndOwnerLogin(oldName, username);
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }

        if (fileRepository.findByFilenameAndOwnerLogin(newName, username) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("File with name '" + newName + "' already exists");
        }

        byte[] content = fileStorage.get(oldName);
        if (content == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read file content");
        }

        fileStorage.put(newName, content);
        fileStorage.remove(oldName);

        file.setFilename(newName);
        fileRepository.save(file);

        return ResponseEntity.ok().build();
    }

    // GET /file/{filename} → скачивание
    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {

        String username = getUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        File file = fileRepository.findByFilenameAndOwnerLogin(filename, username);
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        byte[] content = fileStorage.get(filename);
        if (content == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Создаём Resource
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(content));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(content.length)
                .body(resource);
    }

    // Вспомогательный метод проверки токена
    private boolean isValidToken(String token) {
        if (token == null) return false;
        String cleanToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
        return "forced-test-token".equals(cleanToken);
    }

    // Просто для диплома — токен "forced-test-token" → login "testuser"
    private String getUsernameFromToken(String token) {
        if (token == null) return null;
        String cleanToken = token.replace("Bearer ", "").trim();
        if ("forced-test-token".equals(cleanToken)) {
            return "testuser"; // ← можно сделать маппинг токен → логин
        }
        return null;
    }
}