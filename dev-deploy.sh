#!/bin/bash

set -e

cd ~/Hng_videoSuite_java/
cp ~/second-application.properties ~/Hng_videoSuite_java/src/main/resources/application.properties
# docker compose down -v
docker compose -f compose.yaml up --build -d