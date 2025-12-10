# AI-Pictionary Game

A multiplayer drawing game built in Java where players draw objects and an AI (Google Gemini API) judges their drawings in real-time. This project demonstrates advanced Java concepts including networking, multithreading, GUI development, and database management.

## Project Overview

AI-Pictionary is a client-server application where multiple players can simultaneously draw objects based on prompts. The server uses Google's Gemini API to analyze drawings and determine if players successfully depicted the target object. Each game consists of 5 rounds of drawing attempts, with scoring based on whether the AI correctly identifies the drawing.

## Features

### Core Functionality
- **Multiplayer Support**: Server handles multiple concurrent client connections
- **Real-time Drawing**: Custom Swing-based drawing canvas with smooth mouse tracking
- **AI-Powered Judging**: Integration with Google Gemini 2.5 Flash API for image recognition
- **Game Mechanics**: 
  - 5 rounds per game (1 minute for first round, 15 seconds for subsequent 4 rounds)
  - Score tracking: 1 point for correct identification, 0 points otherwise
  - Automatic game progression and pause on correct guess
- **User Statistics**: SQLite database tracks total games played and total score per user
- **Drawing Tools**: 
  - Multiple color options (Red, Blue, Green, Black)
  - Adjustable brush sizes
  - Clear canvas functionality
  - Each stroke preserves its own color (color changes only affect new strokes)

### Technical Features
- **Concurrent Processing**: Thread pool (ExecutorService) for handling multiple clients
- **Asynchronous API Calls**: Non-blocking Gemini API requests in background threads
- **Persistent Storage**: SQLite database for user statistics
- **Image Encoding**: Base64 encoding for efficient image transmission over network

## Project Structure

```
Final_Project/
├── Client/
│   ├── ClientMain.java      # Main client application with GUI
│   └── DrawingCanvas.java   # Custom drawing canvas component
├── Server/
│   ├── ServerMain.java      # Server entry point
│   ├── ClientHandler.java   # Handles individual client connections
│   ├── GeminiAPI.java       # Google Gemini API integration
│   └── DatabaseManager.java # SQLite database operations
├── lib/
│   └── sqlite-jdbc-3.51.1.0.jar  # SQLite JDBC driver
├── game_database.db         # SQLite database file (auto-generated)
├── Proposal.md              # Original project proposal
└── README.md                # This file
```

## Technologies Used

### Core Java Concepts
- **Networking**: 
  - `Socket` and `ServerSocket` for TCP communication
  - `ObjectInputStream` and `ObjectOutputStream` for object serialization
  - `java.net.http.HttpClient` for REST API calls
- **Multithreading**:
  - `ExecutorService` (CachedThreadPool) for server concurrency
  - Background threads for API calls to prevent blocking
- **GUI Development**:
  - Java Swing (`JFrame`, `JPanel`, `JButton`, etc.)
  - Custom `JPanel` with `paintComponent()` override
  - `Graphics2D` for custom drawing and rendering
  - `BufferedImage` for image capture and processing
- **Database**:
  - JDBC for database connectivity
  - SQLite for lightweight, file-based storage
- **Data Processing**:
  - Base64 encoding/decoding for image transmission
  - JSON string building and parsing for API communication

## Setup Instructions

### Prerequisites
- Java Development Kit (JDK) 8 or higher
- Google Gemini API key (get one from [Google AI Studio](https://makersuite.google.com/app/apikey))

### Configuration

1. **Set API Key**: 
   Edit `Server/GeminiAPI.java` and replace the API key:
   ```java
   private static final String API_KEY = "YOUR_API_KEY_HERE";
   ```

### Compilation

Compile all source files with the SQLite JDBC driver in the classpath:

```bash
# Compile server files
javac -cp "lib/sqlite-jdbc-3.51.1.0.jar" Server/*.java

# Compile client files
javac -cp "lib/sqlite-jdbc-3.51.1.0.jar" Client/*.java
```

### Running the Application

1. **Start the Server** (must be started first):
   ```bash
   java -cp "lib/sqlite-jdbc-3.51.1.0.jar;." ServerMain
   ```
   The server will start on port 8888 and wait for client connections.

2. **Start Client(s)** (can run multiple instances):
   ```bash
   java -cp "lib/sqlite-jdbc-3.51.1.0.jar;." Client.ClientMain
   ```

## How to Play

1. **Connect**: Enter your username and click "Connect to Server"
2. **Receive Prompt**: Server sends a drawing prompt (e.g., "Draw a cat")
3. **Draw**: Use the canvas to draw the object
   - Select colors and brush sizes as needed
   - Each stroke maintains its own color
4. **Submit**: Click "Submit Drawing" or wait for timer to auto-submit
5. **AI Judgment**: AI analyzes your drawing and returns:
   - Identified object name
   - A humorous comment
   - Correct/Incorrect status
6. **Continue**: 
   - If correct: Game pauses, click "New Game" to start a new round
   - If incorrect: Automatically proceed to next round (if rounds remain)
7. **Game End**: After 5 rounds, game ends and score is recorded

## Game Rules

- **One Game = 5 Drawing Rounds**:
  - Round 1: 1 minute to draw
  - Rounds 2-5: 15 seconds each to modify
- **Scoring**:
  - If AI correctly identifies the drawing within 5 rounds: **1 point**
  - If 5 rounds completed without correct identification: **0 points**
  - Clicking "New Game" before winning: **0 points**
- **Statistics**: Total games and total score are tracked per user in the database

## Database Schema

The SQLite database (`game_database.db`) contains:

```sql
CREATE TABLE users (
    username TEXT PRIMARY KEY,
    total_games INTEGER DEFAULT 0,
    total_score INTEGER DEFAULT 0
);
```

## API Integration

The project uses Google Gemini 2.5 Flash API for multimodal analysis:
- **Model**: `gemini-2.5-flash`
- **Endpoint**: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent`
- **Request Format**: JSON with Base64-encoded image and text prompt
- **Response Format**: JSON containing identified object and comment

The AI is provided with a list of all possible drawable objects and identifies what it sees in the drawing, comparing it to the target object on the client side.

## Architecture

```
┌─────────────┐         TCP Socket          ┌─────────────┐
│   Client    │◄───────────────────────────►│   Server    │
│  (Swing)    │    Object Streams           │ (Java)      │
└─────────────┘                              └──────┬──────┘
                                                    │
                                                    │ HTTP/HTTPS
                                                    │ JSON
                                                    ▼
                                            ┌──────────────┐
                                            │ Gemini API   │
                                            │ (External)   │
                                            └──────────────┘
```

## Advanced Concepts Demonstrated

1. **Network Programming**: TCP sockets, object serialization, client-server communication
2. **Multithreading**: Thread pools, concurrent client handling, asynchronous API calls
3. **Advanced GUI**: Custom components, graphics rendering, event handling
4. **Database Management**: JDBC, SQLite integration, persistent data storage
5. **External API Integration**: HTTP client, JSON processing, error handling

## Notes

- Server must be started before clients can connect
- Default server port: **8888**
- Clients and server should be on the same network (or localhost for testing)
- API rate limits may apply (429 errors indicate rate limiting)
- Database file is automatically created on first run

## Future Enhancements

Potential improvements:
- Leaderboard display
- More drawing tools (eraser, shapes)
- Game rooms/lobbies
- Real-time multiplayer in same game session
- Drawing history/replay
- More sophisticated scoring system

## License

This project is developed for educational purposes as part of CS9053 - Introduction to Java course.
