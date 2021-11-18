all: docker

build:
	mvn clean package spring-boot:repackage

docker: build
	docker build -t dbpedia/databus-mods/ontocons .
