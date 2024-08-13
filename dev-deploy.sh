#!/bin/bash

set -e


gunzip -c /tmp/hng_videoSuite_java.tar.gz | docker load
sudo rm -rf /tmp/hng_videoSuite_java.tar.gz
git add .
git stash
git checkout main
git pull origin main

cp ~/second-application.properties ~/Hng_videoSuite_java/src/main/resources/application.properties
# docker compose down -v
docker compose -f compose.yaml up --build -d
