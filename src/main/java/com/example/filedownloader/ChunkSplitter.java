package com.example.filedownloader;

import java.util.ArrayList;
import java.util.List;

public class ChunkSplitter {
    public List<Chunk> split(long contentLength, int chunkSize) {
        if (contentLength <= 0) {
            throw new IllegalArgumentException("Content length must be > 0");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be > 0");
        }

        List<Chunk> chunks = new ArrayList<>();
        for (long start = 0; start < contentLength; start += chunkSize) {
            long end = Math.min(start + chunkSize - 1, contentLength - 1);
            chunks.add(new Chunk(start, end));
        }

        return chunks;
    }
}