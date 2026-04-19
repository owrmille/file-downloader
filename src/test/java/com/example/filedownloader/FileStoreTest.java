package com.example.filedownloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void writeCreatesParentDirectoriesAndWritesBytes() throws Exception {
        FileStore fileStore = new FileStore();
        Path output = tempDir.resolve("nested").resolve("dir").resolve("data.bin");
        byte[] data = new byte[] {1, 2, 3, 4};

        try (OutputStream out = fileStore.openForWrite(output)) {
            out.write(data);
        }

        assertTrue(Files.exists(output));
        assertArrayEquals(data, Files.readAllBytes(output));
    }
}
