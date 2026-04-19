ifneq (,$(wildcard .env))
include .env
export
endif

PROJECT_ROOT ?= $(CURDIR)
TEST_FILES_DIR ?= $(PROJECT_ROOT)/test_files
CONTAINER_NAME ?= fd-httpd
IMAGE ?= httpd:latest
PORT ?= 8080

INPUT_FILE ?=
OUTPUT_DIR ?= $(PROJECT_ROOT)/output_files
CHUNK_SIZE ?=
THREAD_COUNT ?=

.PHONY: help server-up server-down server-logs server-status port-usage run compare run-compare test clean

help:
	@echo "Targets:"
	@echo "  make server-up      Start local Apache server for test_files"
	@echo "  make server-down    Stop local Apache server"
	@echo "  make server-logs    Follow Apache container logs"
	@echo "  make server-status  Show whether container is running"
	@echo "  make port-usage     Show processes using configured PORT"
	@echo "  make run INPUT_FILE=... [CHUNK_SIZE=...] [THREAD_COUNT=...]"
	@echo "      Example: make run INPUT_FILE=medium.txt CHUNK_SIZE=262144 THREAD_COUNT=4"
	@echo "  make compare INPUT_FILE=...  Compare source vs downloaded file (size + hash)"
	@echo "  make run-compare INPUT_FILE=... [CHUNK_SIZE=...] [THREAD_COUNT=...]"
	@echo "  make test           Run Maven tests"
	@echo "  make clean          Run Maven clean"

server-up:
	docker run -d --rm --name $(CONTAINER_NAME) \
		-p $(PORT):80 \
		-v $(TEST_FILES_DIR):/usr/local/apache2/htdocs/ \
		$(IMAGE)
	@echo "Server started: http://localhost:$(PORT)/"

server-down:
	-docker stop $(CONTAINER_NAME)

server-logs:
	docker logs -f $(CONTAINER_NAME)

server-status:
	@docker ps --filter "name=$(CONTAINER_NAME)" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

port-usage:
	@lsof -i :$(PORT) || echo "No process is using port $(PORT)"

run:
	@test -n "$(INPUT_FILE)" || (echo "Usage: make run INPUT_FILE=<file-name> [CHUNK_SIZE=<bytes>] [THREAD_COUNT=<n>]" && exit 1)
	@args="$(INPUT_FILE)"; \
	if [ -n "$(CHUNK_SIZE)" ]; then args="$$args $(CHUNK_SIZE)"; fi; \
	if [ -n "$(THREAD_COUNT)" ]; then args="$$args $(THREAD_COUNT)"; fi; \
	echo "Running with args: $$args"; \
	mvn -q compile exec:java \
		-Dexec.mainClass="com.example.filedownloader.FileDownloaderApp" \
		-Dexec.args="$$args"

compare:
	@test -n "$(INPUT_FILE)" || (echo "Usage: make compare INPUT_FILE=<file-name>" && exit 1)
	@src="$(TEST_FILES_DIR)/$(INPUT_FILE)"; \
	dst="$(OUTPUT_DIR)/$(INPUT_FILE)"; \
	test -f "$$src" || (echo "Source file not found: $$src" && exit 1); \
	test -f "$$dst" || (echo "Downloaded file not found: $$dst" && exit 1); \
	src_size=$$(wc -c < "$$src"); \
	dst_size=$$(wc -c < "$$dst"); \
	src_hash=$$(shasum "$$src" | awk '{print $$1}'); \
	dst_hash=$$(shasum "$$dst" | awk '{print $$1}'); \
	echo "=== Compare Results ==="; \
	echo "Source:      $$src"; \
	echo "Downloaded:  $$dst"; \
	echo "Source size: $$src_size bytes"; \
	echo "Output size: $$dst_size bytes"; \
	echo "Source sha1: $$src_hash"; \
	echo "Output sha1: $$dst_hash"; \
	if [ "$$src_size" = "$$dst_size" ] && [ "$$src_hash" = "$$dst_hash" ]; then \
		echo "MATCH: size and content are identical."; \
	else \
		echo "MISMATCH: size or content differs."; \
		exit 1; \
	fi

run-compare: run compare

test:
	mvn test

clean:
	mvn clean
