# HTTP Server 1.1 From Scratch

A minimal HTTP/1.1 server implementation built from scratch in Kotlin, using only low-level socket APIs without any HTTP frameworks or libraries.

**Note**: This is an educational project. For production use, consider battle-tested HTTP servers like Spring, Ktor, or traditional web servers.


## Project Overview

This project demonstrates how HTTP servers work under the hood by implementing the HTTP/1.1 protocol from first principles. It handles TCP connections, parses HTTP requests, and generates HTTP responses entirely from scratch.

## Features

- Raw TCP socket handling with Java NIO
- HTTP/1.1 request parsing
- Coroutine-based concurrent request handling

## Technology Stack

- **Language**: Kotlin 2.1.0
- **Build Tool**: Gradle 9.1.0
- **Runtime**: JVM 23

## 📋 Prerequisites

- JDK 23 or higher
- Gradle 9.1.0 or higher

## 🚀 Getting Started

### Clone the Repository

```bash
git clone https://github.com/eduardocoutodev/http-server-from-scratch.git
cd http-server-from-scratch
```

### Build the Project

```bash
gradle :app:build
```

### Run the Server

```bash
gradle :app:run
```

The server will start on `http://localhost:444`.

### Test the Server

In another terminal:

```bash
curl http://localhost:444/
```

Or open your browser and navigate to `http://localhost:444/`

## Docker

### Build Docker Image

```bash
docker build -t http-server-from-scratch .
```

### Run with Docker

```bash
docker run -p 444:444 http-server-from-scratch
```

### Pull from Docker Hub

```bash
docker pull eduardooolol/http-server-from-scratch:latest
docker run -p 444:444 eduardooolol/http-server-from-scratch:latest
```

## Configuration

### Server Configuration

The server configuration can be found in `app/src/main/kotlin/ServerContext.kt`:

- **Port**: Default is 444
- **Socket Options**: Reuse address enabled for faster restarts

## Architecture

### Request Flow

1. **TCP Connection**: Server accepts incoming TCP connections on a specified port
2. **Request Parsing**: Raw bytes are parsed into HTTP request objects
3. **Routing**: Request is matched to appropriate handler based on path and method
4. **Processing**: Handler processes the request using coroutines for concurrency
5. **Response Generation**: HTTP response is constructed and sent back to client
6. **Connection Management**: Connection is closed or kept alive based on HTTP headers

## 📧 Contact

Eduardo Couto

* By email: contacts@eduardocouto.dev
* My webpage: [eduardocouto.dev](https://eduardocouto.dev)