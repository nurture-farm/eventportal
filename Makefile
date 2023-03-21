BASE_DIR := $(shell pwd)
JAVA_BIN := $(shell dirname $(shell which java))
NAMESPACE := platform
PROJECT_NAME := event-portal

$(if $(JAVA_BIN),,$(warning "Warning: No Java found in your path, this will probably not work"))

.SILENT: start

.PHONY: all

all: clean package start_local

clean:
	mvn clean

compile:
	mvn compile

package:
	mvn package

install:
	mvn clean install

start_local:
	java -server -Dfile.encoding=UTF-8 -Xms256M -Xmx512M \
		 -DLog4j2ContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector \
		 -Darchaius.configurationSource.additionalUrls=file://$(BASE_DIR)/config/config_local.properties \
		 -classpath "target/event-portal-1.0-SNAPSHOT.jar" farm.nurture.eventportal.server.EventPortalServer

start_dev:
	java -server -Dfile.encoding=UTF-8 -Xms256M -Xmx512M \
		 -DLog4j2ContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector \
		 -Darchaius.configurationSource.additionalUrls=file://$(BASE_DIR)/config/config_dev.properties \
		 -classpath "target/event-portal-1.0-SNAPSHOT.jar" farm.nurture.eventportal.server.EventPortalServer

# Docker related commands
docker_build_local:
	mvn clean package
	docker build -t $(NAMESPACE)/$(PROJECT_NAME):latest --build-arg BUILD_FOR=local .

docker_run:
	docker run --name $(NAMESPACE)_$(PROJECT_NAME) --rm -d -p 3000:8080 $(NAMESPACE)/$(PROJECT_NAME):latest

docker_shell:
	docker exec -it $(NAMESPACE)_$(PROJECT_NAME):latest /bin/sh

docker_container:
	@docker ps -a -q --filter "name=$(NAMESPACE)_$(PROJECT_NAME)"

docker_stop:
	$(eval CONTAINER_ID=$(shell sh -c "docker ps -a -q --filter 'name=$(NAMESPACE)_$(PROJECT_NAME)'"))
	-docker stop $(CONTAINER_ID)

docker_logs:
	$(eval CONTAINER_ID=$(shell sh -c "docker ps -a -q --filter 'name=$(NAMESPACE)_$(PROJECT_NAME)'"))
	-docker logs -f --tail 200 $(CONTAINER_ID)

docker_rm:
	$(eval CONTAINER_ID=$(shell sh -c "docker ps -a -q --filter 'name=$(NAMESPACE)_$(PROJECT_NAME)'"))
	-docker rm $(CONTAINER_ID)

docker_rmi:
	$(eval IMAGE_ID=$(shell sh -c "docker images -q $(NAMESPACE)/$(PROJECT_NAME):latest"))
	-docker rmi $(IMAGE_ID)

docker_rerun: docker_stop docker_rm docker_build_local docker_run docker_logs


# Docker build & Deploy commands
deploy_dev:
	./deploy_container.sh dev

deploy_stage:
	./deploy_container.sh stage

deploy_prod:
	./deploy_container.sh prod master 0.0.17




