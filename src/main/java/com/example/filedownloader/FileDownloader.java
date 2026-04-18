package com.example.filedownloader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class FileDownloader {
    public static void main(String[] args) throws Exception {
        String uri = "http://localhost:8080/word.txt";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).HEAD().build();

        HttpResponse<Void> responseHeader = client.send(request, BodyHandlers.discarding());

        System.out.println(responseHeader.statusCode());
        System.out.println(responseHeader.headers().firstValue("Accept-Ranges").orElse("missing"));
        System.out.println(responseHeader.headers().firstValue("Content-Length").orElse("missing"));
    }
}