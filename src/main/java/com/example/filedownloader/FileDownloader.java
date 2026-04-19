package com.example.filedownloader;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

public class FileDownloader {
    private final FileClient fileClient;
    private final FileStore fileStore;
    private final ChunkSplitter chunkSplitter;

    public FileDownloader(FileClient fileClient, FileStore fileStore, ChunkSplitter chunkSplitter) {
        this.fileClient = fileClient;
        this.fileStore = fileStore;
        this.chunkSplitter = chunkSplitter;
    }

    public void download(String url, Path outputPath, int chunkSize) throws Exception {
        HeadData data = fileClient.head(url);
        validateRangeSupport(data.acceptRanges());

        List<Chunk> chunks = chunkSplitter.split(data.contentLength(), chunkSize);
        long fullBodyLength = 0;

        try (OutputStream out = fileStore.openForWrite(outputPath)) {
            for (Chunk c : chunks) {
                byte[] bytes = fileClient.downloadChunk(url, c);
                out.write(bytes);
                fullBodyLength += bytes.length;
            }
            validateLength(data.contentLength(), fullBodyLength);
        }
    }

    private void validateRangeSupport(String acceptRanges) {
        if (!"bytes".equalsIgnoreCase(acceptRanges)) {
            throw new IllegalStateException(
                    "Server does not advertise byte-range support. Accept-Ranges=" + acceptRanges);
        }
    }

    private void validateLength(long expected, long actual) {
        if (expected != actual) {
            throw new IllegalStateException("Downloaded length does not match Content-Length. expected="
                    + expected + ", actual=" + actual);
        }
    }
}