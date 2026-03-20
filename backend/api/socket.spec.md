# WebSocket Specification

## Overview

The WebSocket infrastructure enables real-time, bidirectional communication between clients and the backend. It provides connection management, automatic reconnection, and group-based message broadcasting for lobbies, games, and other collaborative sessions.

**Version:** 1.0.0
**Last Updated:** 2026-03-21

---

## Architecture

### Core Components

```
Client (WebSocket)
    ↓
GameWebSocketHandler (Spring WebSocket Handler)
    ↓
SessionVerificationService (Authorization)
    ↓
LobbyService (Domain Logic)
    ↓
ConnectionGroup (Connection Management)
    ↓
WebSocketConnectionManager (Registry)
```

### Connection Lifecycle

```
DISCONNECTED → CONNECTING → CONNECTED → RECONNECTING → CONNECTED → DISCONNECTED
                    ↓            ↑
              (verification)   (automatic or manual)
```

---

## Connection Endpoint

### URL Format

```
ws://localhost:8080/ws/player/{playerId}/session/{sessionId}
```

### Parameters

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `playerId` | UUID | Unique player identifier (from `/api/auth/login`) | `550e8400-e29b-41d4-a716-446655440001` |
| `sessionId` | UUID | Session identifier (Lobby ID or Game ID) | `d9c65893-bf77-416c-a9c6-cddd43905215` |

### Connection Requirements

1. **Player must exist** - Obtained from `/api/auth/login` endpoint
2. **Player must be member of session** - Verified by `SessionVerificationService`
3. **Session must be valid** - Lobby or Game ID must exist and be accessible
4. **Valid URI format** - All parameters must be non-null and properly formatted

---

## Connection Flow

### 1. Initial Connection

```
Client connects to: ws://localhost:8080/ws/player/{playerId}/session/{sessionId}
                            ↓
                 GameWebSocketHandler receives connection
                            ↓
                 Extract playerId and sessionId from URI
                            ↓
                 Validate parameters (non-null checks)
                            ↓
                 SessionVerificationService.verifyGroupAccess(playerId, sessionId)
                            ↓
                 LobbyService.getLobbyById(sessionId) → Get ConnectionGroup
                            ↓
                 ConnectionGroup.addConnection(playerId, webSocketSession)
                            ↓
         Send ConnectionResponse: {"status":"CONNECTED",...}
                            ↓
         Player can now send/receive messages
```

### 2. Reconnection (Same Player, New Session)

```
Player 1 was connected with Session A
                            ↓
        Player 1 disconnects (network error, browser close, etc.)
                            ↓
        Player 1 reconnects with new WebSocket session
                            ↓
        GameWebSocketHandler.afterConnectionEstablished(newSession)
                            ↓
        ConnectionGroup.addConnection("player1", newSession)
                            ↓
        OLD SESSION REPLACED with NEW SESSION
        Connection count stays the same (1)
                            ↓
        Other players in group notified (optional)
```

### 3. Disconnection

```
Player WebSocket connection closed
                            ↓
    GameWebSocketHandler.afterConnectionClosed()
                            ↓
    Extract playerId and sessionId from URI
                            ↓
    ConnectionGroup.removeConnection(playerId)
                            ↓
    Broadcast disconnect message to remaining players
    {"type":"PLAYER_DISCONNECTED","playerId":"..."}
                            ↓
    Connection removed from group (count decreases)
```

---

## Message Types

### Connection Messages

#### ConnectionResponse (Server → Client)

Sent immediately after connection attempt.

```json
{
  "status": "CONNECTED|AUTHENTICATION_FAILED|SESSION_INVALID|UNAUTHORIZED",
  "message": "Connection success message or error description",
  "playerId": "550e8400-e29b-41d4-a716-446655440001",
  "sessionId": "d9c65893-bf77-416c-a9c6-cddd43905215",
  "timestamp": 1711000000000
}
```

**Status Values:**
- `CONNECTED` - Successfully authenticated and connected
- `AUTHENTICATION_FAILED` - Missing or invalid playerId
- `SESSION_INVALID` - Session not found or not accessible
- `UNAUTHORIZED` - Player not authorized to access this session

#### Example - Success

```json
{
  "status": "CONNECTED",
  "message": "Connected successfully",
  "playerId": "550e8400-e29b-41d4-a716-446655440001",
  "sessionId": "d9c65893-bf77-416c-a9c6-cddd43905215",
  "timestamp": 1711035690588
}
```

#### Example - Failure

```json
{
  "status": "UNAUTHORIZED",
  "message": "Player is not authorized to access this session",
  "playerId": null,
  "sessionId": null,
  "timestamp": 1711035690588
}
```

### Game Messages (Application-Specific)

Generic message format for application-level communication:

```json
{
  "type": "PLAYER_JOINED|PLAYER_DISCONNECTED|GAME_UPDATE|...",
  "timestamp": 1711035690588,
  "playerId": "550e8400-e29b-41d4-a716-446655440001",
  "sessionId": "d9c65893-bf77-416c-a9c6-cddd43905215",
  "payload": {
    // Application-specific data
  }
}
```

#### Built-in Events

##### PLAYER_CONNECTED

Broadcast to all OTHER players when a new player connects to the session. The connecting player receives a CONNECTED status response instead.

```json
{
  "type": "PLAYER_CONNECTED",
  "timestamp": 1711035690588,
  "playerId": "550e8400-e29b-41d4-a716-446655440001"
}
```

**Broadcast to:** All players in group EXCEPT the connecting player
**When:** Immediately after successful connection establishment
**Purpose:** Notify existing players that a new player has joined the session

##### PLAYER_DISCONNECTED

Broadcast to remaining players when a player disconnects from the session.

```json
{
  "type": "PLAYER_DISCONNECTED",
  "timestamp": 1711035690588,
  "playerId": "550e8400-e29b-41d4-a716-446655440001"
}
```

**Broadcast to:** All players in group EXCEPT the disconnecting player
**When:** When WebSocket connection closes
**Purpose:** Notify remaining players that a player has left the session

---

## Broadcasting

### Broadcast to All Players

Send message to all connected players in a group:

```java
connectionGroup.broadcastAll(messageJson);
```

**Use Cases:**
- Player joined lobby
- Lobby status changed (OPEN → READY)
- Game state updated
- Announcement messages

### Broadcast Excluding One Player

Send message to all except a specific player:

```java
connectionGroup.broadcastAll(messageJson, "excludePlayerId");
```

**Use Cases:**
- Notify others when a player leaves (don't send to leaving player)
- Send disconnect event to remaining players

### Send to Specific Player

Send message to a single player:

```java
connectionGroup.sendTo("playerId", messageJson);
```

**Use Cases:**
- Individual player notifications
- Private messages
- Per-player state updates

---

## Error Handling

### Connection Errors

#### 1. Invalid URI Format

**Cause:** Malformed URL parameters

**Response:**
```json
{
  "status": "AUTHENTICATION_FAILED",
  "message": "Missing playerId or sessionId"
}
```

**Client Action:** Validate URL format before connecting

#### 2. Player Not Found

**Cause:** playerId doesn't exist or was not generated via `/api/auth/login`

**Response:**
```json
{
  "status": "AUTHENTICATION_FAILED",
  "message": "Missing playerId or sessionId"
}
```

**Client Action:** Re-authenticate via `/api/auth/login`

#### 3. Session Not Found

**Cause:** sessionId (Lobby ID) doesn't exist

**Response:**
```json
{
  "status": "SESSION_INVALID",
  "message": "Session not found"
}
```

**Client Action:** Verify lobby ID is correct, check if lobby was deleted

#### 4. Player Not Authorized

**Cause:** Player is not a member of the session

**Response:**
```json
{
  "status": "UNAUTHORIZED",
  "message": "Player is not authorized to access this session"
}
```

**Client Action:** Player must join lobby via `POST /api/lobbies/{lobbyId}/players` first

### Runtime Errors

#### Connection Dropped

**Cause:** Network loss, server restart, timeout

**Behavior:**
1. WebSocket closes with `CloseStatus`
2. `GameWebSocketHandler.afterConnectionClosed()` called
3. Player removed from `ConnectionGroup`
4. Disconnect message broadcast to others

**Client Action:** Attempt reconnection with new WebSocket session

#### Send Failure

**Cause:** Player's WebSocket session no longer open

**Behavior:**
1. `ConnectionGroup.broadcastAll()` skips closed sessions
2. Closed session marked for cleanup on next operation
3. Message reaches all other connected players

**Client Action:** Reconnect automatically if needed

---

## Reconnection Strategy

### Automatic Reconnection Flow

```
Initial Connection Failed (network error, timeout)
                            ↓
        Client waits (exponential backoff)
                            ↓
        Client creates new WebSocket connection
        ws://localhost:8080/ws/player/{playerId}/session/{sessionId}
                            ↓
        New session replaces old one in ConnectionGroup
                            ↓
        Connection count unchanged
                            ↓
        Other players continue normally
```

### Reconnection Detection

The system automatically detects reconnection when:

1. **Same playerId** - Reconnecting player uses same playerId
2. **Same sessionId** - Connecting to same lobby/game
3. **New WebSocketSession** - New network connection with different session object

**Result:** Old session is replaced, no duplicate entries

### Client-Side Reconnection

Recommended client reconnection strategy:

```javascript
const maxRetries = 5;
const baseDelay = 1000; // 1 second

async function connectWithRetry(playerId, sessionId, retryCount = 0) {
  try {
    const ws = new WebSocket(
      `ws://localhost:8080/ws/player/${playerId}/session/${sessionId}`
    );

    ws.onopen = () => {
      console.log('Connected');
      retryCount = 0; // Reset on success
    };

    ws.onerror = async (error) => {
      if (retryCount < maxRetries) {
        const delay = baseDelay * Math.pow(2, retryCount);
        await new Promise(r => setTimeout(r, delay));
        connectWithRetry(playerId, sessionId, retryCount + 1);
      } else {
        console.error('Max retries reached');
      }
    };
  } catch (error) {
    console.error('Connection error:', error);
  }
}
```

---

## State Management

### ConnectionGroup State

```java
public class ConnectionGroup {
  private String groupId;                                    // Lobby/Game ID
  private Map<String, PlayerConnection> playerConnections;  // Active connections

  // Methods
  public PlayerConnection addConnection(String playerId, WebSocketSession session);
  public void removeConnection(String playerId);
  public void broadcastAll(String message);
  public void broadcastAll(String message, String excludePlayerId);
  public void sendTo(String playerId, String message);
  public boolean isPlayerConnected(String playerId);
  public int getConnectionCount();
  public boolean isEmpty();
}
```

### PlayerConnection State

```java
public class PlayerConnection {
  private String playerId;
  private WebSocketSession webSocketSession;
  private LocalDateTime connectedAt;
  private LocalDateTime lastActivityAt;

  public boolean isAlive(); // Returns true if session is open
  public void updateActivity();
}
```

### Connection Tracking

The system tracks:
- **Who's connected:** `ConnectionGroup.isPlayerConnected(playerId)`
- **How many connected:** `ConnectionGroup.getConnectionCount()`
- **Connection metadata:** `PlayerConnection.getConnectedAt()`, `getLastActivityAt()`
- **Session status:** `PlayerConnection.isAlive()`

---

## Integration Points

### With Lobbies

When player joins lobby via `POST /api/lobbies/{lobbyId}/players`:

1. Player added to `Lobby.players` map
2. `ConnectionGroup` created automatically in Lobby constructor
3. Player can now WebSocket connect to that lobby
4. Upon connection, player appears in broadcasts to all group members

```
REST: POST /api/lobbies/{lobbyId}/players
       ↓
Lobby.addPlayer()
       ↓
Lobby has connectionGroup ready
       ↓
WebSocket: ws://localhost:8080/ws/player/{playerId}/session/{lobbyId}
       ↓
ConnectionGroup.addConnection()
       ↓
Broadcasting available
```

### With Game

When game starts:

1. Game receives `ConnectionGroup` from Lobby
2. Game can send updates via `connectionGroup.broadcastAll()`
3. Player disconnections detected automatically
4. Game logic triggered on disconnect (if implemented)

```
Game.start(connectionGroup)
       ↓
Game uses connectionGroup for player communication
       ↓
Players receive real-time game updates
       ↓
Disconnect events handled
```

---

## Usage Examples

### Example 1: Player Joins and Receives Lobby Update

```
1. Client calls POST /api/lobbies/{lobbyId}/players
   Response: LobbyResponse with all current players

2. Client connects WebSocket: ws://localhost:8080/ws/player/player2/session/{lobbyId}
   Response: {"status":"CONNECTED",...}

3. Server broadcasts to all in group:
   {"type":"PLAYER_JOINED","playerId":"player2",...}

4. All connected players receive update and see player2 in lobby
```

### Example 2: Player Network Failure and Reconnection

```
1. Player1 connected to lobby (3 players total)

2. Network drops - WebSocket closes
   → Server calls afterConnectionClosed()
   → connectionGroup.removeConnection("player1")
   → Broadcast {"type":"PLAYER_DISCONNECTED","playerId":"player1",...}
   → Other players see player1 as disconnected

3. Player1's network recovers, reconnects:
   ws://localhost:8080/ws/player/player1/session/{lobbyId}
   → GameWebSocketHandler receives connection
   → connectionGroup.addConnection("player1", newSession)
   → OLD SESSION REPLACED with NEW SESSION
   → Send {"status":"CONNECTED",...}

4. Connection count still 3 (no duplicate)
   → Other players automatically updated
   → Game continues smoothly
```

### Example 3: Broadcasting Game Update

```javascript
// Server-side: Game sends update to all players
const gameUpdateJson = JSON.stringify({
  type: "GAME_UPDATE",
  timestamp: Date.now(),
  playerId: "game-server",
  sessionId: lobbyId,
  payload: {
    playerPositions: {...},
    gameTime: 45000,
    status: "PLAYING"
  }
});

connectionGroup.broadcastAll(gameUpdateJson);

// All connected players receive update in real-time
```

### Example 4: Custom Message Broadcasting

```javascript
// Server-side: Send custom message to specific player
const customMessage = JSON.stringify({
  type: "CUSTOM_NOTIFICATION",
  timestamp: Date.now(),
  payload: {
    title: "You won!",
    points: 1000
  }
});

connectionGroup.sendTo("player1", customMessage);
```

---

## Performance Considerations

### Connection Limits

- **Per Lobby:** No hard limit, but tested with 4+ concurrent connections
- **Per Server:** Depends on system resources (thread pool, memory)
- **Message Rate:** No built-in throttling; implement at application level if needed

### Threading

- **ConcurrentHashMap:** Thread-safe connection storage
- **TextMessage sending:** Serialized per session (Spring WebSocket handles this)
- **No blocking:** Broadcasts use fire-and-forget pattern

### Resource Cleanup

- **Automatic:** Closed sessions are removed from `ConnectionGroup` on disconnect
- **Manual:** `WebSocketConnectionManager.removeConnectionGroup(groupId)` when group is deleted
- **Memory:** Each connection holds minimal state (playerId, timestamp, session reference)

---

## Security

### Authorization Verification

Every connection attempt is verified:

```java
SessionVerificationService.verifyGroupAccess(playerId, sessionId)
  ↓
Checks if player is member of the session
  ↓
Returns false if player not authorized
  ↓
Connection rejected with UNAUTHORIZED status
```

### Information Disclosure

- Player can only access their own `playerId`
- Player can only connect to sessions they're members of
- Broadcast messages include only public information
- No sensitive data transmitted unencrypted (use WSS in production)

### Production Recommendations

1. **Use WSS (WebSocket Secure)** instead of WS
   - Enables TLS/SSL encryption
   - Prevents man-in-the-middle attacks

2. **Implement Rate Limiting**
   - Limit reconnection attempts per minute
   - Limit message frequency per client

3. **Add Message Validation**
   - Validate message format before broadcasting
   - Sanitize content to prevent injection

4. **Monitor Connection Health**
   - Log abnormal disconnect patterns
   - Alert on mass disconnections

---

## Testing

### Unit Tests (83 tests total)

**ConnectionGroupTest (10 tests):**
- Add/remove connections
- Broadcasting (all, exclude)
- Direct messaging
- Reconnection detection
- Connection counting

**GameWebSocketHandlerTest (12 tests):**
- Connection establishment
- Verification failures
- Reconnection scenarios
- Disconnection handling
- Broadcasting on disconnect
- Error responses

**WebSocketConnectionManagerTest (10 tests):**
- Group creation/management
- Multiple groups independence
- Player connection tracking
- Concurrent operations

**SessionVerificationServiceTest (12 tests):**
- Player authorization
- Lobby access control
- Dynamic updates after join/leave

**WebSocketIntegrationTest (39 tests):**
- Sequential player connections
- Multi-player scenarios
- Reconnection flows
- Connection count accuracy
- Complex state transitions

### Integration Testing Checklist

- [ ] Client connects successfully with valid credentials
- [ ] Connection rejected with invalid playerId
- [ ] Connection rejected if player not in lobby
- [ ] Connection rejected if lobby doesn't exist
- [ ] Multiple players can connect to same lobby
- [ ] Reconnection replaces old session without duplicates
- [ ] Disconnect removes player from broadcasts
- [ ] Broadcast reaches all connected players
- [ ] Messages excluded when specified
- [ ] Direct messaging to single player works
- [ ] Connection count accurate after each operation
- [ ] Timestamps recorded correctly
- [ ] Error responses formatted correctly

---

## Future Enhancements

### Planned Features

1. **Heartbeat/Ping-Pong**
   - Periodic health checks
   - Detect stale connections
   - Auto-cleanup of dead sessions

2. **Message History**
   - Persist recent messages
   - Replay for reconnecting players
   - Query historical messages

3. **Advanced Reconnection**
   - Longer session timeout before cleanup
   - Replay missed messages on reconnect
   - Connection state preservation

4. **Distributed WebSocket**
   - Multiple server instances
   - Redis pub/sub for cross-server broadcasting
   - Sticky sessions for reconnection

5. **Rate Limiting**
   - Per-player message limits
   - Reconnection throttling
   - Broadcast throttling

6. **Selective Broadcasting**
   - Subscribe to specific message types
   - Filter broadcasts by payload
   - Role-based message visibility

---

## API Reference

### ConnectionGroup

```java
// Add or reconnect a player
PlayerConnection addConnection(String playerId, WebSocketSession session)

// Remove a player's connection
void removeConnection(String playerId)

// Send to all connected players
void broadcastAll(String message)
void broadcastAll(String message, String excludePlayerId)

// Send to specific player
void sendTo(String playerId, String message)

// Query connection state
boolean isPlayerConnected(String playerId)
int getConnectionCount()
boolean isEmpty()
Collection<PlayerConnection> getActiveConnections()
PlayerConnection getPlayerConnection(String playerId)
String getGroupId()
```

### GameWebSocketHandler

```java
// Called when client connects
void afterConnectionEstablished(WebSocketSession session)

// Called when client sends message (not yet implemented)
void handleTextMessage(WebSocketSession session, TextMessage message)

// Called when client disconnects
void afterConnectionClosed(WebSocketSession session, CloseStatus status)

// Called on transport error
void handleTransportError(WebSocketSession session, Throwable exception)
```

### SessionVerificationService

```java
// Verify player is member of lobby
boolean verifyLobbyAccess(String playerId, String lobbyId)

// Generic group access verification
boolean verifyGroupAccess(String playerId, String groupId)
```

### WebSocketConnectionManager

```java
// Get or create connection group
ConnectionGroup getConnectionGroup(String groupId)
ConnectionGroup createConnectionGroup(String groupId)

// Remove connection group
void removeConnectionGroup(String groupId)

// Check group existence
boolean hasConnectionGroup(String groupId)

// Check player connection status
boolean isPlayerConnected(String playerId, String groupId)
```

---

## Troubleshooting

### "Connection Rejected: UNAUTHORIZED"

**Cause:** Player is not member of the lobby

**Solution:**
1. Ensure player joined lobby via `POST /api/lobbies/{lobbyId}/players`
2. Check that sessionId matches lobby ID
3. Verify player ID is correct from authentication

### "Connection Failed: SESSION_INVALID"

**Cause:** Lobby doesn't exist or was deleted

**Solution:**
1. Verify lobby ID is correct
2. Check if lobby was deleted or expired
3. Create new lobby if needed

### "Frequent Reconnections"

**Cause:** Network instability or server issues

**Solution:**
1. Implement exponential backoff on client
2. Check network connectivity
3. Monitor server resources
4. Consider adding heartbeat mechanism

### "Player Not Visible to Others"

**Cause:** Player connected but broadcast not working

**Solution:**
1. Verify player's connection status: `ConnectionGroup.isPlayerConnected(playerId)`
2. Check ConnectionGroup has other connected players
3. Verify broadcast destination is correct
4. Check for exceptions in message sending

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-03-21 | Initial WebSocket specification |

