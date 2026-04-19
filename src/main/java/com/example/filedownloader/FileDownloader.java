package com.example.filedownloader;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FileDownloader {
    private final FileClient fileClient;
    private final FileStore fileStore;
    private final ChunkSplitter chunkSplitter;

    public FileDownloader(FileClient fileClient, FileStore fileStore, ChunkSplitter chunkSplitter) {
        this.fileClient = fileClient;
        this.fileStore = fileStore;
        this.chunkSplitter = chunkSplitter;
    }

    public void download(String url, Path outputPath, int chunkSize, int threadCount) throws Exception {
        HeadData data = fileClient.head(url);
        validateRangeSupport(data.acceptRanges());

        List<Chunk> chunks = chunkSplitter.split(data.contentLength(), chunkSize);
        try (OutputStream out = fileStore.openForWrite(outputPath)) {
            long fullBodyLength = downloadInParallel(url, chunks, out, threadCount);
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

    private long downloadInParallel(String url, List<Chunk> chunks, OutputStream out, int threadCount)
            throws Exception {

        if (threadCount <= 0) {
            throw new IllegalArgumentException("Thread count must be > 0");
        }

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        try {
            List<Callable<ChunkResult>> tasks = new ArrayList<>();

            for (Chunk c : chunks) {
                tasks.add(() -> {
                    byte[] bytes = fileClient.downloadChunk(url, c);
                    return new ChunkResult(c.start(), bytes);
                });
            }

            List<Future<ChunkResult>> futures = pool.invokeAll(tasks);

            List<ChunkResult> results = new ArrayList<>();
            for (Future<ChunkResult> f : futures) {
                results.add(f.get());
            }

            results.sort(Comparator.comparingLong(ChunkResult::start));

            long totalWritten = 0;
            for (ChunkResult r : results) {
                out.write(r.data());
                totalWritten += r.data().length;
            }
            return totalWritten;
        } finally {
            pool.shutdown();
        }
    }
}