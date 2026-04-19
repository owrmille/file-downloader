package com.example.filedownloader;

import java.nio.file.Path;

public class FileDownloaderApp {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: FileDownloaderApp <input-file-name> <output-path>");
        }

        String baseUrl = "http://localhost:8080/";
        String fileName = args[0];
        String url = baseUrl + fileName;

        Path outputPath = Path.of(args[1]);

        FileClient fileClient = new FileClient();
        FileStore fileStore = new FileStore();
        ChunkSplitter chunkSplitter = new ChunkSplitter();
        FileDownloader downloader = new FileDownloader(fileClient, fileStore, chunkSplitter);

        int chunkSize = 1024;  // TODO: temporary choice -> finalize decision later
        int threadCount = 4;   // TODO: temporary choice -> finalize decision later
        downloader.download(url, outputPath, chunkSize, threadCount);

        System.out.println("File downloaded to: " + outputPath.toAbsolutePath().normalize());
    }
}