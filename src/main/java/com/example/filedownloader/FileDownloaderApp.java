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
        FileDownloader downloader = new FileDownloader(fileClient, fileStore);

        downloader.download(url, outputPath);

        System.out.println("File downloaded to: " + outputPath.toAbsolutePath());
    }
}