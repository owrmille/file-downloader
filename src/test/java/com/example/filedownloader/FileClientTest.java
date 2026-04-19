package com.example.filedownloader;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileClientTest {
    private HttpServer server;
    private FileClient fileClient;
    private byte[] content;

    @BeforeEach
    void setUp() throws Exception {
        content = "integration-content".getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        fileClient = new FileClient();

        server.createContext("/ok", this::handleOk);
        server.createContext("/missing-length", this::handleMissingLength);
        server.createContext("/get-404", this::handleGet404);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void headReadsMetadataFromServer() throws Exception {
        HeadData data = fileClient.head(url("/ok"));

        assertEquals(content.length, data.contentLength());
        assertEquals("bytes", data.acceptRanges());
    }

    @Test
    void getReturnsResponseBodyBytes() throws Exception {
        byte[] downloaded = fileClient.get(url("/ok"));

        assertArrayEquals(content, downloaded);
    }

    @Test
    void headFailsWhenContentLengthIsMissing() {
        assertThrows(IllegalStateException.class, () -> fileClient.head(url("/missing-length")));
    }

    @Test
    void getFailsOnNon200Status() {
        assertThrows(IllegalStateException.class, () -> fileClient.get(url("/get-404")));
    }

    private String url(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    private void handleOk(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
        exchange.getResponseHeaders().set("Content-Length", String.valueOf(content.length));

        if ("HEAD".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(405, -1);
        exchange.close();
    }

    private void handleMissingLength(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        exchange.getResponseHeaders().set("Accept-Ranges", "bytes");

        if ("HEAD".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(405, -1);
        exchange.close();
    }

    private void handleGet404(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }
}
