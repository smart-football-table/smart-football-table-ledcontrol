docker run -it --rm \
--name my-maven-project \
-v "$(pwd)":/usr/src/mymaven \
-w /usr/src/mymaven maven:3.6-jdk-8 \
mvn clean package jib:buildTar && cat target/jib-image.tar | docker image load

