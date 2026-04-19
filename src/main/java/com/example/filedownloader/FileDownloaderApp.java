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

        System.out.println("File downloaded to: " + outputPath.toAbsolutePath().normalize());
    }

//    public static void main(String[] args) throws Exception {
//        String baseUrl = "http://localhost:8080/";
//        String fileName = args.length > 0 ? args[0] : "groceries.txt";
//        String url = baseUrl + fileName;
//
//        FileClient fileClient = new FileClient();
//
//        Chunk chunk = new Chunk(0, 15); // first 16 bytes
//        byte[] bytes = fileClient.downloadChunk(url, chunk);
//
//        System.out.println("Requested range: " + chunk.toRangeHeader());
//        System.out.println("Expected length: " + chunk.length());
//        System.out.println("Downloaded length: " + bytes.length);
//        System.out.println("As text: " + new String(bytes));
//    }
}