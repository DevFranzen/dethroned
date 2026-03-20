# Dethroned Game Backend - API Documentation

Complete API specification for the Dethroned Game Backend covering both REST and WebSocket communication.

## 📚 Documentation Files

### REST API
**File:** `openapi.yaml`

OpenAPI 3.0 specification for synchronous REST endpoints.

**Sections:**
- **Authentication** - Player login and ID generation
- **Lobbies** - Lobby creation, management, and listing
- **Players** - Player management within lobbies
- **Game Settings** - Game configuration

**Key Endpoints:**
- `POST /api/auth/login` - Get player ID
- `POST /api/lobbies` - Create lobby
- `GET /api/lobbies` - Get open lobbies
- `GET /api/lobbies/{lobbyId}` - Get lobby details
- `DELETE /api/lobbies/{lobbyId}` - Delete lobby
- `POST /api/lobbies/{lobbyId}/players` - Add player to lobby
- `POST /api/lobbies/{lobbyId}/players/{playerId}/ready` - Mark player ready
- `POST /api/lobbies/{lobbyId}/players/{playerId}/leave` - Player leaves lobby

---

### WebSocket API
**File:** `asyncapi.yaml`

AsyncAPI 3.0 specification for asynchronous WebSocket endpoints.

**Endpoint:** `ws://localhost:8080/ws/player/{playerId}/session/{sessionId}`

**Messages:**
- Connection establishment and verification
- Player join/leave notifications
- Game state updates
- Custom application messages

**Key Features:**
- Real-time bidirectional communication
- Automatic player authorization verification
- Reconnection support with session replacement
- Group-based message broadcasting

---

### WebSocket Architecture Guide
**File:** `socket.spec.md`

Comprehensive guide covering WebSocket infrastructure, connection flows, and integration patterns.

**Sections:**
- Architecture overview with component diagrams
- Detailed connection flows (initial, reconnection, disconnection)
- Message types and formats
- Broadcasting patterns
- Error handling and recovery
- State management
- Integration with lobbies and games
- Performance and security considerations
- Testing strategies

---

## 🔗 API Integration Flow

### Phase 1: REST API (Authentication & Lobby Management)

```
Client                          Backend
  │
  ├─→ POST /api/auth/login
  │   ← playerId (UUID)
  │
  ├─→ POST /api/lobbies
  │   {"playerId": "...", "gameSettings": {...}}
  │   ← LobbyResponse {"id": "...", "code": "...", "players": [...]}
  │
  ├─→ POST /api/lobbies/{lobbyId}/players
  │   {"playerId": "...", "username": "...", "role": "PLAYER"}
  │   ← LobbyResponse (updated lobby with all players)
  │
  └─→ POST /api/lobbies/{lobbyId}/players/{playerId}/ready
      ← Acknowledgment
```

### Phase 2: WebSocket API (Real-Time Communication)

```
Client                                      Backend
  │
  ├─→ ws://localhost:8080/ws/player/{playerId}/session/{lobbyId}
  │                                            ↓
  │                        [Verify player authorization]
  │                                            ↓
  │←─ ConnectionResponse {"status": "CONNECTED", ...}
  │
  │   [Other players receive notification]
  │←─────────────────────────────────────────────────────
  │       {"type":"PLAYER_CONNECTED","playerId":"..."}
  │
  │   [Real-time game updates]
  │←─────────────────────────────────────────────────────
  │       {"type":"GAME_UPDATE","payload":{...}}
  │
  │   [When player disconnects]
  │←─────────────────────────────────────────────────────
  │       {"type":"PLAYER_DISCONNECTED","playerId":"..."}
```

---

## 🔒 Authentication & Authorization

### REST API Authentication
- **Player ID Generation:** Anonymous login via `POST /api/auth/login`
- **No credentials required:** Fresh UUID generated on each login
- **Use playerId in subsequent requests:** Include in request bodies and headers

### WebSocket Authentication
- **Player verification:** playerId must be from `/api/auth/login`
- **Session authorization:** Player must be member of target session
- **Automatic verification:** `SessionVerificationService` checks authorization on connection
- **Failed connections:** Rejected with detailed error status

---

## 📊 Message Flow Examples

### Example 1: Player Joins Lobby

```
REST Phase:
  POST /api/lobbies/lobby-123/players
  Body: {"playerId":"player2","username":"Alice","role":"PLAYER"}
  Response: LobbyResponse with all players

WebSocket Phase:
  Client connects: ws://localhost:8080/ws/player/player2/session/lobby-123

  Server verifies player2 is in lobby-123

  player2 receives: {"status":"CONNECTED",...}
  player1 receives: {"type":"PLAYER_CONNECTED","playerId":"player2",...}
```

### Example 2: Game State Update

```
WebSocket:
  Game sends to all connected players:
  {
    "type":"GAME_UPDATE",
    "timestamp":1711035690588,
    "payload":{
      "playerPositions":{"player1":{x:100,y:200},"player2":{x:300,y:150}},
      "gameTime":45000,
      "status":"PLAYING"
    }
  }

  All players receive state synchronously
```

### Example 3: Reconnection After Network Failure

```
Initial Connection:
  player1 connects → Receives CONNECTED response → Participates in game

Network Failure:
  WebSocket closes → Server calls afterConnectionClosed()
  → Other players receive PLAYER_DISCONNECTED
  → player1 appears offline in lobby

Reconnection:
  player1 connects again with same playerId/sessionId
  → New WebSocket session replaces old one
  → Connection count unchanged (no duplicate)
  → Server responds CONNECTED
  → Seamless reconnection for other players
```

---

## 🛠️ Implementation Checklist

### For Frontend Developers
- [ ] Implement authentication flow (`POST /api/auth/login`)
- [ ] Implement lobby creation and listing
- [ ] Implement lobby join (REST endpoint)
- [ ] Implement WebSocket connection with error handling
- [ ] Implement reconnection with exponential backoff
- [ ] Parse and handle connection response
- [ ] Handle player joined/left notifications
- [ ] Handle game state updates
- [ ] Implement graceful disconnect handling
- [ ] Validate all incoming WebSocket messages

### For Backend Developers
- [ ] Verify WebSocket authentication works correctly
- [ ] Monitor connection health and resource usage
- [ ] Test reconnection scenarios
- [ ] Verify broadcast messages reach correct recipients
- [ ] Handle edge cases (rapid connect/disconnect, network errors)
- [ ] Implement metrics/logging for connections
- [ ] Document custom application messages
- [ ] Plan for distributed system (multiple servers)

### For DevOps/Infrastructure
- [ ] Configure WSS (WebSocket Secure) in production
- [ ] Set up load balancing for WebSocket connections (sticky sessions)
- [ ] Monitor connection pools and resource limits
- [ ] Configure proper timeouts and keepalive
- [ ] Set up alerting for connection anomalies
- [ ] Plan for WebSocket upgrade path (Redis pub/sub, etc.)

---

## 📈 Performance & Scalability

### Current Architecture
- **Thread Model:** Concurrent connection handling with ConcurrentHashMap
- **Broadcasting:** In-memory, fire-and-forget pattern
- **Scalability:** Single server, suitable for local dev and small deployments
- **Tested With:** Up to 4 concurrent connections per lobby

### Production Considerations
- **Use WSS:** Always use WebSocket Secure in production
- **Load Balancing:** Sticky sessions required (same server for reconnection)
- **Distributed Broadcasting:** Plan for Redis pub/sub when scaling
- **Connection Limits:** Monitor thread pool exhaustion
- **Message Rate Limits:** Implement throttling if needed
- **Heartbeat:** Consider adding ping/pong for connection health

---

## 🧪 Testing

### Unit Tests (92 tests)
- ConnectionGroup messaging
- WebSocket handler lifecycle
- Verification service
- Connection manager
- Integration scenarios

### Integration Tests
- Multi-player connection flows
- Reconnection scenarios
- Broadcast verification
- State management
- Error handling

**Run tests:** `./mvnw clean test`

---

## 🔍 Debugging Tips

### WebSocket Connection Issues

**"AUTHENTICATION_FAILED"**
- Check that playerId is valid UUID format
- Verify playerId was obtained from `/api/auth/login`
- Confirm URL is correctly formatted

**"SESSION_INVALID"**
- Verify sessionId (lobby ID) exists
- Check if lobby was deleted
- Confirm URL format is correct

**"UNAUTHORIZED"**
- Ensure player joined lobby via REST API first
- Check player is in the target lobby
- Verify sessionId matches lobby ID

### Connection Drops
- Check network connectivity
- Monitor server logs for exceptions
- Verify client reconnection logic
- Check for server resource exhaustion
- Monitor thread pool utilization

### Message Not Received
- Verify sender has connected successfully
- Check recipient is in same session/group
- Confirm message format is valid JSON
- Check for broadcast filtering (exclude rules)
- Verify no exceptions in message sending

---

## 📖 API Standards

### REST API
- **Standard:** OpenAPI 3.0
- **Format:** JSON request/response
- **Status Codes:** Standard HTTP (200, 201, 400, 403, 404, 409)
- **Error Format:** `{"error":"message","timestamp":milliseconds}`

### WebSocket API
- **Standard:** AsyncAPI 3.0
- **Protocol:** WebSocket (ws/wss)
- **Message Format:** JSON with type field
- **No status codes:** Use message types for routing
- **Bidirectional:** Both client→server and server→client messages

---

## 🚀 Getting Started

### 1. Run Backend Server
```bash
./mvnw spring-boot:run
```

### 2. Test with curl (REST)
```bash
# Get Player ID
curl -X POST http://localhost:8080/api/auth/login

# Create Lobby
curl -X POST http://localhost:8080/api/lobbies \
  -H "Content-Type: application/json" \
  -d '{
    "playerId":"<player-id>",
    "gameSettings":{"minPlayers":2,"maxPlayers":4,"gameMode":"elimination","timeLimit":0,"fieldWidth":800,"fieldHeight":600}
  }'
```

### 3. Connect WebSocket (Browser DevTools)
```javascript
// In browser console
const ws = new WebSocket(
  'ws://localhost:8080/ws/player/<playerId>/session/<lobbyId>'
);

ws.onopen = () => console.log('Connected');
ws.onmessage = (e) => console.log('Message:', JSON.parse(e.data));
ws.onerror = (e) => console.log('Error:', e);
ws.onclose = () => console.log('Closed');
```

---

## 📝 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-03-21 | Initial API specification with OpenAPI and AsyncAPI |

---

## 📞 Support & Contributions

For questions, issues, or contributions:
- **Team:** Dethroned Development Team
- **Documentation:** See spec files (openapi.yaml, asyncapi.yaml, socket.spec.md)
- **Tests:** 92 unit and integration tests for reference

---

## 📋 Related Documentation

- `openapi.yaml` - REST API specification
- `asyncapi.yaml` - WebSocket API specification
- `socket.spec.md` - WebSocket architecture guide
- `pom.xml` - Maven dependencies
- `CLAUDE.md` - Development guidelines
