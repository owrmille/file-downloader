package com.example.filedownloader;

import java.nio.file.Path;

public class FileDownloader {
    private final FileClient fileClient;
    private final FileStore fileStore;

    public FileDownloader(FileClient fileClient, FileStore fileStore) {
        this.fileClient = fileClient;
        this.fileStore = fileStore;
    }

    public void download(String url, Path outputPath) throws Exception {
        HeadData data = fileClient.head(url);
        validateRangeSupport(data.acceptRanges());

        byte[] body = fileClient.get(url);
        validateLength(data.contentLength(), body.length);

        fileStore.write(outputPath, body);
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