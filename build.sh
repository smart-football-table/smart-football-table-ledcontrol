#!/bin/sh

`dirname "$0"`/mvnw clean package jib:buildTar && docker image load -i target/jib-image.tar

