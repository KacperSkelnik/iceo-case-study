#!/bin/bash

sbt --batch -Dsbt.server.forcestart=true assembly
docker-compose up --build