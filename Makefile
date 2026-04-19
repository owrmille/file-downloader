ifneq (,$(wildcard .env))
include .env
export
endif

PROJECT_ROOT ?= $(CURDIR)
TEST_FILES_DIR ?= $(PROJECT_ROOT)/test_files
CONTAINER_NAME ?= fd-httpd
IMAGE ?= httpd:latest
PORT ?= 8080

.PHONY: help server-up server-down server-logs server-status port-usage run test clean

help:
	@echo "Targets:"
	@echo "  make server-up      Start local Apache server for test_files"
	@echo "  make server-down    Stop local Apache server"
	@echo "  make server-logs    Follow Apache container logs"
	@echo "  make server-status  Show whether container is running"
	@echo "  make port-usage     Show processes using configured PORT"
	@echo "  make run FILE=... [OUT=...]  Run FileDownloaderApp with args"
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
	@mvn -q exec:java \
		-Dexec.mainClass="com.example.filedownloader.FileDownloaderApp" \
		-Dexec.args="$(or $(FILE),groceries.txt) $(or $(OUT),./output_files/$(or $(FILE),groceries.txt))"

test:
	mvn test

clean:
	mvn clean