package ru.netology.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ← новое поле: уникальный ID

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "size")
    private Long size;

    @Column(name = "owner_login", nullable = false)
    private String ownerLogin;

    // Конструкторы, геттеры, сеттеры
    public File() {}

    // Геттеры и сеттеры...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getOwnerLogin() { return ownerLogin; }
    public void setOwnerLogin(String ownerLogin) { this.ownerLogin = ownerLogin; }
}