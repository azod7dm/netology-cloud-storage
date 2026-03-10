package ru.netology.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.netology.entity.File;
import ru.netology.repository.FileRepository;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    // Имитация in-memory хранилища для теста
    private final Map<String, byte[]> fileStorage = new HashMap<>();

    @Test
    void shouldRenameFileSuccessfully() {
        // given
        String oldName = "old.txt";
        String newName = "new.txt";
        String owner = "testuser";
        byte[] content = "test data".getBytes();

        File file = new File();
        file.setFilename(oldName);
        file.setOwnerLogin(owner);
        file.setSize((long) content.length);

        // Подготовка моков
        when(fileRepository.findByFilenameAndOwnerLogin(oldName, owner)).thenReturn(file);
        when(fileRepository.findByFilenameAndOwnerLogin(newName, owner)).thenReturn(null);
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> {
            File saved = invocation.getArgument(0);
            assertNotNull(saved);
            return saved;
        });

        // Прямая имитация хранилища в сервисе (через рефлексию или через внедрение — здесь напрямую)
        try {
            var field = FileService.class.getDeclaredField("fileStorage");
            field.setAccessible(true);
            field.set(fileService, fileStorage);
        } catch (Exception e) {
            fail("Не удалось установить fileStorage: " + e.getMessage());
        }

        // Загружаем файл вручную перед тестом
        fileStorage.put(oldName, content);

        // when
        assertDoesNotThrow(() -> fileService.renameFile(oldName, newName, owner));

        // then
        assertTrue(fileStorage.containsKey(newName));
        assertFalse(fileStorage.containsKey(oldName));
        assertEquals(newName, file.getFilename());
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