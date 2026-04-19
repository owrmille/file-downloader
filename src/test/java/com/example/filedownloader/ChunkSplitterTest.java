package com.example.filedownloader;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkSplitterTest {

    @Test
    void splitCreatesExactChunksWhenEvenlyDivisible() {
        ChunkSplitter splitter = new ChunkSplitter();

        List<Chunk> chunks = splitter.split(12, 4);

        assertEquals(List.of(
                new Chunk(0, 3),
                new Chunk(4, 7),
                new Chunk(8, 11)
        ), chunks);
    }

    @Test
    void splitCreatesShortLastChunkWhenNotEvenlyDivisible() {
        ChunkSplitter splitter = new ChunkSplitter();

        List<Chunk> chunks = splitter.split(10, 4);

        assertEquals(List.of(
                new Chunk(0, 3),
                new Chunk(4, 7),
                new Chunk(8, 9)
        ), chunks);
    }

    @Test
    void splitRejectsInvalidInput() {
        ChunkSplitter splitter = new ChunkSplitter();

        assertThrows(IllegalArgumentException.class, () -> splitter.split(0, 4));
        assertThrows(IllegalArgumentException.class, () -> splitter.split(10, 0));
    }
}
