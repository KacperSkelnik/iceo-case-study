# ICEO CASE STUDY

## Description

This project contains:
- RabbitMQ producer that load csv file into fs2 stream and split it into separate streams base on result of modulo operation.
Numbers form that stream are then summed and publish to the right rabbitmq queue.
```aidl
<one of file values> % <modulo divider from config> --> stream of numbers with the same result 
``` 

- Websocket api that allow connect to one of the queues.
```aidl
ws://localhost:8080/ws?number=2
```

## How to run

To run this application it is convenient to use bash script `run.sh` from the root directory.
```bash
./run.sh
```
`run.sh` content:
```bash
#!/bin/bash

sbt --batch -Dsbt.server.forcestart=true assembly
docker-compose up --build
```
This build jar with dependencies and run docker compose to run all necessary services:
- RabbitMQ server
- Publisher
- Websocket API

Publisher and websocket api to run locally (without docker) requires following environmental variables:

### Publisher

|     ENV NAME    |  EXAMPLE |
|:---------------:|:--------:|
|    FILE_PATH    | test.csv |
|  MODULO_DIVIDER |     5    |
|   RABBIT_HOST   | rabbitmq |
| RABBIT_USERNAME |   guest  |
| RABBIT_PASSWORD |   guest  |

### Websocket API

|     ENV NAME    |  EXAMPLE |
|:---------------:|:--------:|
|   RABBIT_HOST   | rabbitmq |
| RABBIT_USERNAME |   guest  |
| RABBIT_PASSWORD |   guest  |