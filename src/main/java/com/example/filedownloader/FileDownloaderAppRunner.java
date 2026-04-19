package com.example.filedownloader;

import java.nio.file.Path;

public class FileDownloaderAppRunner {
    public void run(String[] args) throws Exception {
        AppArguments appArguments = AppArguments.parse(args);

        String baseUrl = "http://localhost:8080/";
        String fileName = appArguments.fileName();
        String url = baseUrl + fileName;

        Path outputPath = Path.of("./output_files").resolve(fileName);

        FileClient fileClient = new FileClient();
        FileStore fileStore = new FileStore();
        ChunkSplitter chunkSplitter = new ChunkSplitter();
        FileDownloader downloader = new FileDownloader(fileClient, fileStore, chunkSplitter);

        int chunkSize = appArguments.chunkSize();
        int threadCount = appArguments.threadCount();

        downloader.download(url, outputPath, chunkSize, threadCount);

        System.out.println("Input file: " + fileName);
        System.out.println("Chunk size: " + chunkSize + " bytes");
        System.out.println("Thread count: " + threadCount);
        System.out.println("File successfully downloaded to: " + outputPath.toAbsolutePath().normalize());
    }
}