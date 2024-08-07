# Video Suite Application
This application is designed for handling video processing. This application is designed to process and encode videos. It utilizes FFmpeg for video encoding and RabbitMQ for message brokering. The application can be run using Docker Compose or connected to a local database

## Table of Contents

- [Prerequisites](#prerequisites)
- [Features](#features)
- [Setup](#setup)
- [Usage](#usage)
- [Testing](#testing)
- [Contributing](#contributing)

## Prerequisites

Before running the application, ensure you have the following installed:

- Java 17 or later: Required to run the application
- FFmpeg: Required to process the video
- Rabbitmq: Required for message brokering. Can be run using docker of installed locally.
- postgres: Required to persist data and update progress status
- Docker: required if using docker compose to start application

## Features
- Video Encoding: Encodes videos using FFmpeg.
- Video Concatenation: concatenates two or more videos using FFmpeg.
- Progress Tracking: Updates job progress during video encoding. 
- Message Queue: Uses RabbitMQ for handling video processing jobs.

## Setup

### Clone the Repository

```bash
git clone <repository-url>
cd <repository-directory>
```

### Using Docker Compose

To start the application using Docker Compose:

1. provide the environmental in the compose file:
2. create a docker profile .properties file to hold app configs
3. Start the application:

```bash
docker-compose up -d
```

### Without Docker

1. Ensure you have rabbimq and postgres installed locally.
2. update application.properties

    ```properties
    # Rabbitmq configuration
    spring.rabbitmq.host=${HOST}
    spring.rabbitmq.port=5672
    spring.rabbitmq.username=${USER}
    spring.rabbitmq.password=${PASSWORD}
    
    # postgres configuration
    spring.datasource.url=jdbc:${CONNECTION_STRING}
    spring.datasource.username=${DB_USER}
    spring.datasource.password=${DB_PASS}
    
    rabbitmq.queue.mergedVideos=${MergeQueueName}
    rabbitmq.queue.concat=${concatQueueName}
    ```

3. Build and run the Spring Boot application:

```bash
./mvnw spring-boot:run
```
   
## Usage
- Start the Application 
- Follow the instructions in the Setup and Installation section to start the application. 
- Send Video Encoding Jobs 
- The application expects video encoding jobs to be sent to RabbitMQ. Ensure you have configured the RabbitMQ queue names correctly in your application settings. 
- Check Job Progress 
- Job progress will be updated and logged as the encoding progresses.

# Testing
To run tests:
```bash
./mvnw test
```

## Contributing

If you would like to contribute to this project, please follow these steps:

1. Fork the repository.
2. clone the forked repository 
3. Create a new branch (`git checkout -b feature/your-feature`). 
4. Commit your changes (`git commit -m 'Add new feature'`). 
5. Push to the branch (`git push origin feature/your-feature`). 
6. Create a new Pull Request.

---