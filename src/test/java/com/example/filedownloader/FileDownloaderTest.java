package com.example.filedownloader;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDownloaderTest {

    @Test
    void downloadWritesBodyWhenMetadataIsValid() throws Exception {
        byte[] body = "hello".getBytes();
        StubFileClient fileClient = new StubFileClient(new HeadData(body.length, "bytes"), body);
        CapturingFileStore fileStore = new CapturingFileStore();
        ChunkSplitter chunkSplitter = new ChunkSplitter();
        FileDownloader downloader = new FileDownloader(fileClient, fileStore, chunkSplitter);

        Path outputPath = Path.of("output_files/test.txt");
        int chunkSize = 1024;
        int threadCount = 2;
        downloader.download("http://localhost:8080/test.txt", outputPath, chunkSize, threadCount);

        assertEquals(outputPath, fileStore.writtenPath);
        assertArrayEquals(body, fileStore.writtenBytes);
    }

    @Test
    void downloadFailsWhenRangeSupportIsMissing() {
        byte[] body = "hello".getBytes();
        StubFileClient fileClient = new StubFileClient(new HeadData(body.length, "none"), body);
        CapturingFileStore fileStore = new CapturingFileStore();
        ChunkSplitter chunkSplitter = new ChunkSplitter();
        FileDownloader downloader = new FileDownloader(fileClient, fileStore, chunkSplitter);

        int chunkSize = 1024;
        int threadCount = 2;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> downloader.download("http://localhost:8080/test.txt", Path.of("out.txt"), chunkSize, threadCount));
        assertTrue(ex.getMessage().contains("Accept-Ranges"));
    }

    @Test
    void downloadFailsWhenBodyLengthDiffersFromContentLength() {
        byte[] body = "hello".getBytes();
        StubFileClient fileClient = new StubFileClient(new HeadData(body.length + 1L, "bytes"), body);
        CapturingFileStore fileStore = new CapturingFileStore();
        ChunkSplitter chunkSplitter = new ChunkSplitter();
        FileDownloader downloader = new FileDownloader(fileClient, fileStore, chunkSplitter);

        int chunkSize = 1024;
        int threadCount = 2;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> downloader.download("http://localhost:8080/test.txt", Path.of("out.txt"), chunkSize, threadCount));
        assertTrue(ex.getMessage().contains("Downloaded length does not match Content-Length"));
    }

    @Test
    void downloadFailsWhenThreadCountIsNotPositive() {
        byte[] body = "hello".getBytes();
        StubFileClient fileClient = new StubFileClient(new HeadData(body.length, "bytes"), body);
        CapturingFileStore fileStore = new CapturingFileStore();
        ChunkSplitter chunkSplitter = new ChunkSplitter();
        FileDownloader downloader = new FileDownloader(fileClient, fileStore, chunkSplitter);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> downloader.download("http://localhost:8080/test.txt", Path.of("out.txt"), 1024, 0));
        assertTrue(ex.getMessage().contains("Thread count must be > 0"));
    }

    @Test
    void downloadFailsWhenChunkSizeIsNotPositive() {
        byte[] body = "hello".getBytes();
        StubFileClient fileClient = new StubFileClient(new HeadData(body.length, "bytes"), body);
        CapturingFileStore fileStore = new CapturingFileStore();
        ChunkSplitter chunkSplitter = new ChunkSplitter();
        FileDownloader downloader = new FileDownloader(fileClient, fileStore, chunkSplitter);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> downloader.download("http://localhost:8080/test.txt", Path.of("out.txt"), 0, 2));
        assertTrue(ex.getMessage().contains("Chunk size must be > 0"));
    }

    @Test
    void downloadFailsForZeroLengthContent() {
        byte[] body = new byte[0];
        StubFileClient fileClient = new StubFileClient(new HeadData(0, "bytes"), body);
        CapturingFileStore fileStore = new CapturingFileStore();
        ChunkSplitter chunkSplitter = new ChunkSplitter();
        FileDownloader downloader = new FileDownloader(fileClient, fileStore, chunkSplitter);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> downloader.download("http://localhost:8080/empty", Path.of("out.txt"), 1024, 2));
        assertTrue(ex.getMessage().contains("Content length must be > 0"));
    }

    private static class StubFileClient extends FileClient {
        private final HeadData headData;
        private final byte[] body;

        private StubFileClient(HeadData headData, byte[] body) {
            this.headData = headData;
            this.body = body;
        }

        @Override
        public HeadData head(String url) {
            return headData;
        }

        @Override
        public byte[] downloadChunk(String url, Chunk chunk) {
            int from = (int) chunk.start();
            int toExclusive = (int) chunk.end() + 1;
            if (from >= body.length) {
                return new byte[0];
            }
            toExclusive = Math.min(toExclusive, body.length);
            return Arrays.copyOfRange(body, from, toExclusive);
        }
    }

    private static class CapturingFileStore extends FileStore {
        private Path writtenPath;
        private byte[] writtenBytes;

        @Override
        public OutputStream openForWrite(Path outputPath) {
            this.writtenPath = outputPath;
            return new ByteArrayOutputStream() {
                @Override
                public void close() {
                    writtenBytes = this.toByteArray();
                }
            };
        }
    }
}
