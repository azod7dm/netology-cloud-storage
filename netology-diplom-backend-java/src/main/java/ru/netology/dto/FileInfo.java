package ru.netology.dto;

public class FileInfo {
    private String filename;
    private Long size;

    public FileInfo() {}

    public FileInfo(String filename, Long size) {
        this.filename = filename;
        this.size = size;
    }

    // Getters and Setters
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}