package com.example.filedownloader;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
        server.createContext("/range-returns-200", this::handleRangeReturns200);
        server.createContext("/range-length-mismatch", this::handleRangeLengthMismatch);
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
    void downloadChunkReturnsOnlyRequestedRange() throws Exception {
        Chunk chunk = new Chunk(2, 6);

        byte[] downloaded = fileClient.downloadChunk(url("/ok"), chunk);

        assertArrayEquals(Arrays.copyOfRange(content, 2, 7), downloaded);
    }

    @Test
    void headFailsWhenContentLengthIsMissing() {
        assertThrows(IllegalStateException.class, () -> fileClient.head(url("/missing-length")));
    }

    @Test
    void getFailsOnNon200Status() {
        assertThrows(IllegalStateException.class, () -> fileClient.get(url("/get-404")));
    }

    @Test
    void downloadChunkFailsWhenServerReturns200ForRangeRequest() {
        Chunk chunk = new Chunk(0, 4);

        assertThrows(IllegalStateException.class,
                () -> fileClient.downloadChunk(url("/range-returns-200"), chunk));
    }

    @Test
    void downloadChunkFailsWhenReturnedBodyLengthDoesNotMatchRangeLength() {
        Chunk chunk = new Chunk(0, 4);

        assertThrows(IllegalStateException.class,
                () -> fileClient.downloadChunk(url("/range-length-mismatch"), chunk));
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
            String rangeHeader = exchange.getRequestHeaders().getFirst("Range");
            if (rangeHeader != null) {
                Chunk chunk = parseRange(rangeHeader);
                byte[] chunkData = Arrays.copyOfRange(content, (int) chunk.start(), (int) chunk.end() + 1);

                exchange.getResponseHeaders().set("Content-Length", String.valueOf(chunkData.length));
                exchange.getResponseHeaders().set(
                        "Content-Range",
                        "bytes " + chunk.start() + "-" + chunk.end() + "/" + content.length);
                exchange.sendResponseHeaders(206, chunkData.length);
                exchange.getResponseBody().write(chunkData);
                exchange.close();
                return;
            }

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

    private void handleRangeReturns200(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(content.length));
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(405, -1);
        exchange.close();
    }

    private void handleRangeLengthMismatch(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            byte[] mismatched = new byte[] {1, 2}; // shorter than requested in test
            exchange.getResponseHeaders().set("Accept-Ranges", "bytes");
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(mismatched.length));
            exchange.sendResponseHeaders(206, mismatched.length);
            exchange.getResponseBody().write(mismatched);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(405, -1);
        exchange.close();
    }

    private Chunk parseRange(String rangeHeader) {
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            throw new IllegalArgumentException("Invalid Range header: " + rangeHeader);
        }

        String[] bounds = rangeHeader.substring("bytes=".length()).split("-");
        if (bounds.length != 2) {
            throw new IllegalArgumentException("Invalid Range header: " + rangeHeader);
        }

        long start = Long.parseLong(bounds[0]);
        long end = Long.parseLong(bounds[1]);
        return new Chunk(start, end);
    }
}
