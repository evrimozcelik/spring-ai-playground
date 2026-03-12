# Travel Agent - Spring AI Project

A Spring AI-powered travel agent application that helps users discover and plan their trips using AI.

## Features

- 🤖 AI-powered travel recommendations using OpenAI
- 🗺️ Travel destination database with 10 popular destinations
- 🔍 Vector search for semantic destination matching
- 💬 Conversational interface with memory
- 🛠️ Tool calling for destination queries

## Technologies

- Spring Boot 3.5.11
- Spring AI 1.1.2
- OpenAI (gpt-4o-mini for chat, text-embedding-3-small for embeddings)
- H2 Database (in-memory)
- Vector Store for semantic search

## Prerequisites

- Java 25
- OpenAI API Key (set as environment variable `SPRING_AI_OPENAI_API_KEY`)

## Running the Application

```bash
./mvnw clean package
./mvnw spring-boot:run
```

## API Usage

### Chat with Travel Agent

```bash
curl -X POST http://localhost:8080/{username}/travel \
  -H "Content-Type: text/plain" \
  -d "I want to visit a tropical beach destination"
```

## Destination Categories

- City
- Beach
- Mountain
- Island
- Adventure
- Nature

## Sample Destinations

- Paris, France
- Bali, Indonesia
- Tokyo, Japan
- Maldives
- Swiss Alps, Switzerland
- New York, USA
- Santorini, Greece
- Amazon Rainforest, Brazil
- Dubai, UAE
- Iceland
