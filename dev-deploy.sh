#!/bin/bash

set -e

cd ~/Hng_videoSuite_java/
cp ~/docker-application.properties ~/Hng_videoSuite_java/src/main/resources/application.properties
cp ~/.env ~/Hng_videoSuite_java/
# docker compose down -v
docker compose -f compose.yaml up --build -d