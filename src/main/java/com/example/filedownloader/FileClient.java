package com.example.filedownloader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class FileClient {
    private final HttpClient httpClient;

    public FileClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public HeadData head(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).HEAD().build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        validateStatus(response.statusCode(), "HEAD");

        String acceptRanges = response.headers().firstValue("Accept-Ranges").orElse("missing");

        Optional<String> contentLengthHeader = response.headers().firstValue("Content-Length");
        if (contentLengthHeader.isEmpty()) {
            throw new IllegalStateException("HEAD response does not contain Content-Length");
        }

        long contentLength;
        try {
            contentLength = Long.parseLong(contentLengthHeader.get());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid Content-Length: " + contentLengthHeader.get(), e);
        }

        return new HeadData(contentLength, acceptRanges);
    }

    public byte[] get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        validateStatus(response.statusCode(), "GET");
        return response.body();
    }

    private void validateStatus(int status, String method) {
        if (status != 200) {
            throw new IllegalStateException(method + " failed with status: " + status);
        }
    }

    public byte[] downloadChunk(String url, Chunk chunk) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Range", chunk.toRangeHeader())
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 206) {
            throw new IllegalStateException("Expected 206 for range request, got " + response.statusCode());
        }

        byte[] body = response.body();
        if (body.length != chunk.length()) {
            throw new IllegalStateException("Dowloaded (chunk) length does not match expected length. expected="
                    + chunk.length() + ", actual=" + body.length);
        }

        return body;
    }
}