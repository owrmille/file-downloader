package com.example.filedownloader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;

public class FileDownloader {
    public static void main(String[] args) throws Exception {
        String uri = "http://localhost:8080/word.txt";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest headRequest = HttpRequest.newBuilder(URI.create(uri)).HEAD().build();

        HttpResponse<Void> headResponseHeader = client.send(headRequest, BodyHandlers.discarding());

        int headStatus = headResponseHeader.statusCode();
        String headAcceptRanges = headResponseHeader.headers().firstValue("Accept-Ranges").orElse("missing");
        Optional<String> headContentLengthHeader = headResponseHeader.headers().firstValue("Content-Length");

        System.out.println(headStatus);
        System.out.println(headAcceptRanges);
        System.out.println(headContentLengthHeader.orElse("missing"));

        if (headStatus != 200) {
            throw new IllegalStateException("HEAD failed with status " + headStatus);
        }
        if (headContentLengthHeader.isEmpty()) {
            throw new IllegalStateException("HEAD response does not contain Content-Length");
        }
    }
}