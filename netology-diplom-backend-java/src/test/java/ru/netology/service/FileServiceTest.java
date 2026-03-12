package ru.netology.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.netology.entity.File;
import ru.netology.repository.FileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Добавьте эти импорты ↓↓↓
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    private final String TEST_USER = "testuser";
    private final Path storageRoot = Paths.get("./storage_mocks");

    // Подменяем путь через рефлексию
    private void setStoragePath(String path) throws Exception {
        var field = FileService.class.getDeclaredField("rootPath");
        field.setAccessible(true);
        field.set(fileService, Paths.get(path).toAbsolutePath().normalize());
    }

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(storageRoot);
        setStoragePath("./storage_mocks");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(storageRoot)) {
            Files.walk(storageRoot)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        }
    }

    @Test
    void shouldRenameFileSuccessfully() throws IOException {
        // given
        String oldName = "old.txt";
        String newName = "new.txt";
        Path userDir = storageRoot.resolve(TEST_USER);
        Files.createDirectories(userDir);
        Files.write(userDir.resolve(oldName), "data".getBytes());

        File file = new File();
        file.setFilename(oldName);
        file.setOwnerLogin(TEST_USER);
        file.setSize(4L);

        when(fileRepository.findByFilenameAndOwnerLogin(oldName, TEST_USER)).thenReturn(file);
        when(fileRepository.findByFilenameAndOwnerLogin(newName, TEST_USER)).thenReturn(null);
        when(fileRepository.save(any(File.class))).thenAnswer(i -> i.getArgument(0));

        // when
        fileService.renameFile(oldName, newName, TEST_USER);

        // then
        assertTrue(Files.exists(userDir.resolve(newName)));
        assertFalse(Files.exists(userDir.resolve(oldName)));

        verify(fileRepository).save(argThat(f -> f.getFilename().equals(newName)));
    }

    @Test
    void shouldThrowWhenRenamingToExistingName() {
        // given
        when(fileRepository.findByFilenameAndOwnerLogin("old.txt", "user")).thenReturn(new File());
        when(fileRepository.findByFilenameAndOwnerLogin("new.txt", "user")).thenReturn(new File());

        // when + then
        Exception exception = assertThrows(RuntimeException.class, () -> {
            fileService.renameFile("old.txt", "new.txt", "user");
        });

        assertTrue(exception.getMessage().contains("already exists"));
    }
}