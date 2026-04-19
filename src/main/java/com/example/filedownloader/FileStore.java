package com.example.filedownloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileStore {
    public void write(Path outputPath, byte[] data) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(outputPath, data);
    }
}