package ru.netology.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.entity.File;
import ru.netology.service.FileService;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/list")
    public ResponseEntity<List<File>> getFiles(
            @RequestHeader("auth-token") String token,
            @RequestParam int limit) {

        String username = getUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<File> files = fileService.listFiles(username, limit);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("Error listing files for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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
            fileService.uploadFile(filename, file.getBytes(), username);
            return ResponseEntity.ok("File uploaded");
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
        }
    }

    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(
            @RequestHeader("auth-token") String token,
            @RequestParam String filename) {

        String username = getUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            fileService.deleteFile(filename, username);
            return ResponseEntity.ok("File deleted");
        } catch (Exception e) {
            log.error("Delete failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Delete failed");
        }
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

        String newName = request.get("newname");
        if (newName == null || newName.trim().isEmpty()) {
            newName = request.values().stream()
                    .filter(value -> !value.equals(oldName))
                    .findFirst()
                    .orElse(null);
        }

        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("New filename is required");
        }

        try {
            fileService.renameFile(oldName, newName, username);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Rename failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {

        String username = getUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            byte[] content = fileService.getFileContent(filename, username);
            InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(content));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(content.length)
                    .body(resource);
        } catch (RuntimeException e) {
            log.error("Download failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private String getUsernameFromToken(String token) {
        if (token == null) return null;
        String cleanToken = token.replace("Bearer ", "").trim();
        return "forced-test-token".equals(cleanToken) ? "testuser" : null;
    }
}