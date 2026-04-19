package com.example.filedownloader;

public record AppArguments(String fileName, int chunkSize, int threadCount) {
    private static final int DEFAULT_CHUNK_SIZE = 1024;
    private static final int DEFAULT_THREAD_COUNT = 4;

    public static AppArguments parse(String[] args) {
        if (args.length < 1 || args.length > 3) {
            throw new IllegalArgumentException(
                    "Usage: FileDownloaderApp <input-file-name> [chunk-size-bytes] [thread-count]");
        }

        String fileName = args[0];
        int chunkSize = args.length >= 2
                ? parsePositiveInt(args[1], "chunk-size-bytes")
                : DEFAULT_CHUNK_SIZE;
        int threadCount = args.length >= 3
                ? parsePositiveInt(args[2], "thread-count")
                : DEFAULT_THREAD_COUNT;

        return new AppArguments(fileName, chunkSize, threadCount);
    }

    private static int parsePositiveInt(String rawValue, String fieldName) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " must be > 0");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a positive integer: " + rawValue, e);
        }
    }
}