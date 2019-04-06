#!/bin/sh

DOCKER_IMAGE=maven:3.6-jdk-8

docker run -it --rm -u "$(id -u)" \
--name my-maven-project \
-v "$(pwd)":/usr/src/mymaven \
-w /usr/src/mymaven \
$DOCKER_IMAGE \
mvn clean package jib:buildTar && cat target/jib-image.tar | docker image load

