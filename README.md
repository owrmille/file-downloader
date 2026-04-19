# File Downloader

Java file downloader that fetches a file from an HTTP server using byte ranges, downloads chunks in parallel, and merges them into a final output file.

## Features

- `HEAD` metadata fetch (`Accept-Ranges`, `Content-Length`)
- Byte-range chunk splitting
- Parallel chunk download with thread pool
- Ordered merge of chunks
- Size validation after merge
- Unit/integration-style tests with JUnit 5

## Tech Stack

- Java 21
- Maven
- JUnit 5
- Local test server: Docker `httpd`

## Project Structure

- `src/main/java/com/example/filedownloader/FileDownloaderApp.java` — tiny entrypoint
- `src/main/java/com/example/filedownloader/FileDownloaderAppRunner.java` — app run flow (setup + orchestration)
- `src/main/java/com/example/filedownloader/AppArguments.java` — CLI argument parsing/validation
- `src/main/java/com/example/filedownloader/FileDownloader.java` — core downloader logic
- `src/main/java/com/example/filedownloader/FileClient.java` — HTTP client operations (`HEAD`, range `GET`)
- `src/main/java/com/example/filedownloader/ChunkSplitter.java` — chunk planning
- `src/main/java/com/example/filedownloader/FileStore.java` — output stream/file writing

## Requirements

- Java 21+
- Maven 3.9+
- Docker (for local web server)
- `make` (optional, for convenience commands)

## Local Setup

1. Put files to download into `test_files/`.
2. Start local HTTP server:

```bash
make server-up
```

Files become available at:

- `http://localhost:8080/<file-name>`

Example:

- `http://localhost:8080/groceries.txt`

## Run

Primary (tool-independent) way:

```bash
mvn compile exec:java \
  -Dexec.mainClass=com.example.filedownloader.FileDownloaderApp \
  -Dexec.args="groceries.txt 262144 4"
```

- `groceries.txt` = input file name
- `262144` = chunk size in bytes
- `4` = thread count

Optional convenience (requires `make`):

`INPUT_FILE` is required in Make targets.

### Basic run

```bash
make run INPUT_FILE=groceries.txt
```

### Run with custom chunk size and thread count

```bash
make run INPUT_FILE=medium.txt CHUNK_SIZE=262144 THREAD_COUNT=4
```

Notes:

- `CHUNK_SIZE` is in bytes.
- If `CHUNK_SIZE` / `THREAD_COUNT` are omitted, app defaults are used.

## Compare Output with Source

Primary way (without Make):

```bash
wc -c test_files/groceries.txt output_files/groceries.txt
shasum test_files/groceries.txt output_files/groceries.txt
```

Optional convenience (requires `make`):

### Compare only

```bash
make compare INPUT_FILE=groceries.txt
```

### Run + compare in one command

```bash
make run-compare INPUT_FILE=groceries.txt CHUNK_SIZE=262144 THREAD_COUNT=4
```

Comparison checks:

- file size
- SHA-1 hash

## Tests

Primary:

```bash
mvn clean test
```

Optional convenience:

```bash
make clean && make test
```

Current tests cover:

- chunk splitting logic
- downloader behavior and validations
- file writing behavior
- HTTP client behavior (`HEAD`, `GET`, range `GET`) via in-memory local server
- edge cases (invalid args, invalid statuses, range mismatch, etc.)

## Stop Local Server

```bash
docker stop fd-httpd
```

Or with Make: 
```bash
make server-down
```


## Useful Make Targets

(`make` required)

```bash
make help
make server-status
make server-logs
make port-usage
```
