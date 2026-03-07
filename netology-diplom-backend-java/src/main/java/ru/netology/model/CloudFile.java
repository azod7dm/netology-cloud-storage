package ru.netology.model;

import jakarta.persistence.*;

@Entity
@Table(name = "files")
public class CloudFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "filename")
    private String filename;

    @Column(name = "size")
    private Long size;

    @Column(name = "upload_date")
    private java.time.LocalDateTime uploadDate;

    @Column(name = "owner_login")
    private String ownerLogin;

    public CloudFile() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public java.time.LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(java.time.LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public String getOwnerLogin() { return ownerLogin; }
    public void setOwnerLogin(String ownerLogin) { this.ownerLogin = ownerLogin; }
}