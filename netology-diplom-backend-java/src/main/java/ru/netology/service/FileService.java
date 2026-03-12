package ru.netology.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import jakarta.annotation.PostConstruct;
import ru.netology.entity.File;
import ru.netology.repository.FileRepository;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static java.nio.file.StandardOpenOption.*;

@Service
public class FileService {

    @Value("${app.file-storage-path:./storage}")
    private String storagePath;

    private Path rootPath;

    private final FileRepository fileRepository;

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @PostConstruct
    public void init() {
        rootPath = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать корневую папку хранилища: " + rootPath, e);
        }
    }

    public List<File> listFiles(String ownerLogin, int limit) {
        return fileRepository.findByOwnerLogin(ownerLogin)
                .stream().limit(limit).toList();
    }

    public void uploadFile(String filename, byte[] content, String ownerLogin) {
        Path userDir = rootPath.resolve(ownerLogin);
        try {
            Files.createDirectories(userDir);

            Path filePath = userDir.resolve(filename).normalize();
            if (!filePath.startsWith(userDir)) {
                throw new RuntimeException("Попытка выхода за пределы домашней директории");
            }

            Files.write(filePath, content, CREATE, TRUNCATE_EXISTING);

            File dbFile = new File();
            dbFile.setFilename(filename);
            dbFile.setSize((long) content.length);
            dbFile.setOwnerLogin(ownerLogin);
            fileRepository.save(dbFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file on disk: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteFile(String filename, String ownerLogin) {
        fileRepository.deleteByFilenameAndOwnerLogin(filename, ownerLogin);

        Path userDir = rootPath.resolve(ownerLogin);
        Path filePath = userDir.resolve(filename).normalize();

        if (filePath.startsWith(userDir)) {
            try {
                FileSystemUtils.deleteRecursively(filePath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete file from disk: " + e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void renameFile(String oldName, String newName, String ownerLogin) {
        File file = fileRepository.findByFilenameAndOwnerLogin(oldName, ownerLogin);
        if (file == null) {
            throw new RuntimeException("File not found");
        }

        if (fileRepository.findByFilenameAndOwnerLogin(newName, ownerLogin) != null) {
            throw new RuntimeException("File with name '" + newName + "' already exists");
        }

        Path userDir = rootPath.resolve(ownerLogin);
        Path oldPath = userDir.resolve(oldName).normalize();
        Path newPath = userDir.resolve(newName).normalize();

        if (!oldPath.startsWith(userDir) || !newPath.startsWith(userDir)) {
            throw new RuntimeException("Invalid path");
        }

        try {
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to rename file on disk", e);
        }

        file.setFilename(newName);
        fileRepository.save(file);
    }

    public byte[] getFileContent(String filename, String ownerLogin) {
        File dbFile = fileRepository.findByFilenameAndOwnerLogin(filename, ownerLogin);
        if (dbFile == null) {
            throw new RuntimeException("File not found");
        }

        Path userDir = rootPath.resolve(ownerLogin);
        Path filePath = userDir.resolve(filename).normalize();

        if (!filePath.startsWith(userDir)) {
            throw new RuntimeException("Invalid path");
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content", e);
        }
    }
}