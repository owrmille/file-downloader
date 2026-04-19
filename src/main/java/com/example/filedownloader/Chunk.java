package com.example.filedownloader;

public record Chunk(long start, long end) {
    public Chunk {
        if (start < 0) {
            throw new IllegalArgumentException("Start must be >= 0");
        }
        if (end < start) {
            throw new IllegalArgumentException("End must be >= start");
        }
    }

    public long length() {
        return end - start + 1;
    }

    public String toRangeHeader() {
        return "bytes=" + start + "-" + end;
    }
}