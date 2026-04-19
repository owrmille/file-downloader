package com.example.filedownloader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppArgumentsTest {

    @Test
    void parseUsesDefaultsWhenOnlyFileNameProvided() {
        AppArguments args = AppArguments.parse(new String[] {"input.txt"});

        assertEquals("input.txt", args.fileName());
        assertEquals(1024, args.chunkSize());
        assertEquals(4, args.threadCount());
    }

    @Test
    void parseUsesCustomChunkAndThreadValues() {
        AppArguments args = AppArguments.parse(new String[] {"input.txt", "2048", "8"});

        assertEquals("input.txt", args.fileName());
        assertEquals(2048, args.chunkSize());
        assertEquals(8, args.threadCount());
    }

    @Test
    void parseRejectsInvalidArgCount() {
        assertThrows(IllegalArgumentException.class, () -> AppArguments.parse(new String[] {}));
        assertThrows(IllegalArgumentException.class,
                () -> AppArguments.parse(new String[] {"a.txt", "1", "2", "3"}));
    }

    @Test
    void parseRejectsNonPositiveOrNonNumericNumbers() {
        assertThrows(IllegalArgumentException.class,
                () -> AppArguments.parse(new String[] {"a.txt", "0"}));
        assertThrows(IllegalArgumentException.class,
                () -> AppArguments.parse(new String[] {"a.txt", "-1"}));
        assertThrows(IllegalArgumentException.class,
                () -> AppArguments.parse(new String[] {"a.txt", "abc"}));

        assertThrows(IllegalArgumentException.class,
                () -> AppArguments.parse(new String[] {"a.txt", "1024", "0"}));
        assertThrows(IllegalArgumentException.class,
                () -> AppArguments.parse(new String[] {"a.txt", "1024", "x"}));
    }
}
