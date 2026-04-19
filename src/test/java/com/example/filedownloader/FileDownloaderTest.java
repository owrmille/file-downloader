package com.example.filedownloader;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

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
        FileDownloader downloader = new FileDownloader(fileClient, fileStore);

        Path outputPath = Path.of("output_files/test.txt");
        downloader.download("http://localhost:8080/test.txt", outputPath);

        assertEquals(outputPath, fileStore.writtenPath);
        assertArrayEquals(body, fileStore.writtenBytes);
    }

    @Test
    void downloadFailsWhenRangeSupportIsMissing() {
        byte[] body = "hello".getBytes();
        StubFileClient fileClient = new StubFileClient(new HeadData(body.length, "none"), body);
        CapturingFileStore fileStore = new CapturingFileStore();
        FileDownloader downloader = new FileDownloader(fileClient, fileStore);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> downloader.download("http://localhost:8080/test.txt", Path.of("out.txt")));
        assertTrue(ex.getMessage().contains("Accept-Ranges"));
    }

    @Test
    void downloadFailsWhenBodyLengthDiffersFromContentLength() {
        byte[] body = "hello".getBytes();
        StubFileClient fileClient = new StubFileClient(new HeadData(body.length + 1L, "bytes"), body);
        CapturingFileStore fileStore = new CapturingFileStore();
        FileDownloader downloader = new FileDownloader(fileClient, fileStore);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> downloader.download("http://localhost:8080/test.txt", Path.of("out.txt")));
        assertTrue(ex.getMessage().contains("Downloaded length does not match Content-Length"));
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
        public byte[] get(String url) {
            return body;
        }
    }

    private static class CapturingFileStore extends FileStore {
        private Path writtenPath;
        private byte[] writtenBytes;

        @Override
        public void write(Path outputPath, byte[] data) {
            this.writtenPath = outputPath;
            this.writtenBytes = data;
        }
    }
}
