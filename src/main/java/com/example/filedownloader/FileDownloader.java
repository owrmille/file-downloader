package com.example.filedownloader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FileDownloader {
    public static void main(String[] args) throws Exception {
        String uri = "http://localhost:8080/word.txt";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest headRequest = HttpRequest.newBuilder(URI.create(uri)).HEAD().build();

        HttpResponse<Void> headResponseHeader = client.send(headRequest, BodyHandlers.discarding());

        int headStatus = headResponseHeader.statusCode();
        String headAcceptRanges = headResponseHeader.headers().firstValue("Accept-Ranges").orElse("missing");
        Optional<String> headContentLength = headResponseHeader.headers().firstValue("Content-Length");

        System.out.println(headStatus);
        System.out.println(headAcceptRanges);
        System.out.println(headContentLength.orElse("missing"));

        if (headStatus != 200) {
            throw new IllegalStateException("HEAD failed with status " + headStatus);
        }
        if (headContentLength.isEmpty()) {
            throw new IllegalStateException("HEAD response does not contain Content-Length");
        }

        long expectedContentLength = Long.parseLong(headContentLength.get());

        HttpRequest getRequest = HttpRequest.newBuilder(URI.create(uri)).GET().build();
        HttpResponse<byte[]> getResponse = client.send(getRequest, BodyHandlers.ofByteArray());

        int getStatus = getResponse.statusCode();
        String getAcceptRanges = getResponse.headers().firstValue("Accept-Ranges").orElse("missing");
        long downloadedLength = getResponse.body().length;

        System.out.println(getStatus);
        System.out.println(getAcceptRanges);
        System.out.println(downloadedLength);

        if (getStatus == 200) {
            byte[] body = getResponse.body();
            if (body.length == expectedContentLength) {
                Path outputDir = Path.of("./output_files");
                Files.createDirectories(outputDir);
                Path outputFile = outputDir.resolve("output.txt");
                Files.write(outputFile, body);
            } else {
                throw new IllegalStateException(
                        "Downloaded length does not match HEAD Content-Length. expected="
                                + expectedContentLength + ", actual=" + body.length);
            }
        } else {
            throw new IllegalStateException("GET failed with status " + getStatus);
        }
    }
}