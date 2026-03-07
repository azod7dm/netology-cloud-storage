package ru.netology.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;

import jakarta.annotation.PostConstruct;
import ru.netology.dto.FileInfo;
import ru.netology.model.CloudFile;
import ru.netology.repository.CloudFileRepository;
import ru.netology.service.AuthService;

import java.io.IOException;
import java.nio.file.*;

@Service
@Lazy
public class FileService {

    @Value("${app.file-storage-path:./uploads}")
    private String storagePath;

    private final CloudFileRepository fileRepository;
    private final AuthService authService;

    public FileService(CloudFileRepository fileRepository, AuthService authService) {
        this.fileRepository = fileRepository;
        this.authService = authService;
    }

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(storagePath).toAbsolutePath().normalize();
            Files.createDirectories(path);
            System.out.println("Папка хранилища создана: " + path);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать папку хранилища: " + storagePath, e);
        }
    }

    public List<FileInfo> listFiles(String token) {
        if (!authService.isTokenValid(token)) return null;
        String login = authService.getLoginByToken(token);
        return fileRepository.findByOwnerLogin(login).stream()
                .map(file -> new FileInfo(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());
    }

    public void saveFile(String token, String filename, byte[] content) throws IOException {
        if (!authService.isTokenValid(token)) return;
        String login = authService.getLoginByToken(token);

        Path filePath = Paths.get(storagePath, login, filename);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        CloudFile file = new CloudFile();
        file.setOwnerLogin(login);
        file.setFilename(filename);
        file.setSize((long) content.length);
        file.setUploadDate(java.time.LocalDateTime.now());
        fileRepository.save(file);
    }

    public byte[] getFile(String token, String filename) throws IOException {
        if (!authService.isTokenValid(token)) return null;
        String login = authService.getLoginByToken(token);

        Path filePath = Paths.get(storagePath, login, filename);
        if (Files.exists(filePath)) {
            return Files.readAllBytes(filePath);
        }
        return null;
    }

    public void deleteFile(String token, String filename) throws IOException {
        if (!authService.isTokenValid(token)) return;
        String login = authService.getLoginByToken(token);

        Path filePath = Paths.get(storagePath, login, filename);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
        fileRepository.deleteByOwnerLoginAndFilename(login, filename);
    }
}