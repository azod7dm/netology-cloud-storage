package ru.netology.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.netology.entity.File;
import ru.netology.repository.FileRepository;

import java.util.List;
import java.util.Map;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final Map<String, byte[]> fileStorage = new java.util.HashMap<>();

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public List<File> listFiles(String ownerLogin, int limit) {
        return fileRepository.findByOwnerLogin(ownerLogin)
                .stream().limit(limit).toList();
    }

    public void uploadFile(String filename, byte[] content, String ownerLogin) {
        fileStorage.put(filename, content);

        File dbFile = new File();
        dbFile.setFilename(filename);
        dbFile.setSize((long) content.length);
        dbFile.setOwnerLogin(ownerLogin);
        fileRepository.save(dbFile);
    }

    @Transactional
    public void deleteFile(String filename, String ownerLogin) {
        fileRepository.deleteByFilenameAndOwnerLogin(filename, ownerLogin);
        fileStorage.remove(filename);
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

        byte[] content = fileStorage.get(oldName);
        if (content == null) {
            throw new RuntimeException("Failed to read file content");
        }

        fileStorage.put(newName, content);
        fileStorage.remove(oldName);

        file.setFilename(newName);
        fileRepository.save(file);
    }

    public byte[] getFileContent(String filename, String ownerLogin) {
        File file = fileRepository.findByFilenameAndOwnerLogin(filename, ownerLogin);
        if (file == null) {
            throw new RuntimeException("File not found");
        }
        return fileStorage.get(filename);
    }
}