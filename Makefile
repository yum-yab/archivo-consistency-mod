all: docker

build:
	mvn clean spring-boot:repackage

docker: build
	docker build -t dbpedia/databus-mods/ontocons .
