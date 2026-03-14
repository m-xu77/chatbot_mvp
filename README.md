# Java LLM Chat MVP

A minimal Java web application that streams LLM responses in real time using [OpenRouter](https://openrouter.ai/) and a clean chat UI .

## Demo

Type a message and watch the response stream in token by token, just like ChatGPT.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.3, Spring WebFlux |
| HTTP Client | WebClient (non-blocking) |
| Streaming | Server-Sent Events (SSE) |
| Frontend | Vanilla HTML / CSS / JavaScript |
| LLM API | OpenRouter (any supported model) |

## Features

- Real-time streaming responses via SSE
- Multi-turn conversation (full history sent with each request)
- Supports any model available on OpenRouter
- Graceful error handling (rate limits, auth errors, etc.)
- Zero frontend dependencies — single HTML file

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- An [OpenRouter API key](https://openrouter.ai/keys)

### Run

```bash
OPENROUTER_API_KEY=your-key-here mvn spring-boot:run
```

Open `http://localhost:8080` in your browser.

### Change Model

Edit `src/main/resources/application.properties`:

```properties
openrouter.model=minimax/minimax-m2.5
```

Replace with any model slug from the [OpenRouter models page](https://openrouter.ai/models).

## Project Structure

```
src/main/
├── java/com/mvp/
│   ├── MvpApplication.java          # Entry point
│   ├── controller/ChatController.java   # POST /api/chat → SSE stream
│   ├── service/OpenRouterService.java   # WebClient → OpenRouter API
│   └── model/ChatRequest.java           # Request body
└── resources/
    ├── application.properties           # Config (API key, model)
    └── static/index.html                # Chat UI
```

## API

**`POST /api/chat`**

Request body:
```json
{
  "messages": [
    { "role": "user", "content": "Hello!" }
  ]
}
```

Response: `text/event-stream` — streams content tokens as SSE events.
