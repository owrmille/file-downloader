package com.example.filedownloader;

public record ChunkResult(long start, byte[] data) {
}