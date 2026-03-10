package ru.netology.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.netology.entity.File;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, String> {
    List<File> findByOwnerLogin(String ownerLogin);
    File findByFilenameAndOwnerLogin(String filename, String ownerLogin);
    void deleteByFilenameAndOwnerLogin(String filename, String ownerLogin);
}