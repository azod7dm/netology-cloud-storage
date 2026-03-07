package ru.netology.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.netology.model.CloudFile;

import java.util.List;

@Repository
public interface CloudFileRepository extends JpaRepository<CloudFile, Long> {
    List<CloudFile> findByOwnerLogin(String ownerLogin);
    void deleteByOwnerLoginAndFilename(String ownerLogin, String filename);
}