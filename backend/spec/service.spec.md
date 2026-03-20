# Dethroned Game Backend - Service Specification

## 1. Überblick

**Dethroned** ist ein Browser-basiertes Multiplayer-Spiel mit dezentralisierter Controller-Architektur:
- **Game Display**: Web-Browser (Desktop/Tablet) zeigt das Spielfeld
- **Player Controller**: Mobile Apps (iOS/Android) für Spieler-Eingaben
- **Backend**: Spring Boot Service koordiniert alle Verbindungen und Spiellogik
- **Kommunikation**: WebSocket für Echtzeit-Datenaustausch

## 2. Funktionale Anforderungen

### 2.1 Lobby-Management
- **Lobby-Erstellung**: Host kann eine neue Lobby mit eindeutiger ID erstellen
- **Lobby-Beitritt**: Spieler können via Code/Link einer offenen Lobby beitreten
- **Lobby-Status**: Lobbies können folgende Zustände haben: `OPEN` (wartet auf Spieler), `READY` (alle Spieler bereit), `IN_PROGRESS` (Spiel läuft), `FINISHED` (Spiel vorbei)
- **Spieler-Management**: Host kann Spieler entfernen, Lobby schließen
- **Automatisches Cleanup**: Leere/inaktive Lobbies werden nach konfigurierbarer Zeit gelöscht

### 2.2 Spieler-Verwaltung
- **Session**: Jeder Client (Display + Controller) hat eindeutige Session-ID
- **Benutzertypen**:
  - **Display-Client**: Zeigt Spielfeld, erhält Game-State Updates
  - **Player-Client**: Kontrolliert einen Spieler, sendet Eingaben
  - **Host/Spectator**: Optional - Spieler ohne aktive Spielfigur (Zuschauer)
- **Identifikation**: Jeder Client ist über Username oder eindeutige ID identifizierbar
- **Status**: CONNECTED, READY, PLAYING, DISCONNECTED

### 2.3 Spiel-Management
- **Game-State**: Zentraler Zustand enthält:
  - Aktive Spieler und ihre Positionen/Status
  - Spielfeld-Elemente (Obstacles, Items, etc.)
  - Aktuelle Phase (Setup, In-Progress, End)
  - Scorer/Punkte
- **Game-Flow**:
  - Setup Phase: Spieler verbinden, bestätigen Ready
  - Start Condition: Display startet Spiel wenn genug Spieler
  - Live Updates: Display erhält kontinuierliche Game-State Updates
  - End Condition: Spiel endet nach Bedingung (Zeit, Ziel erreicht, etc.)
- **Spieler-Eingaben**:
  - Player-Clients senden Befehle (Movement, Actions)
  - Server validiert und verarbeitet Befehle
  - Game-State wird entsprechend aktualisiert
  - Alle Clients erhalten Update via Broadcast

### 2.4 WebSocket-Kommunikation
- **Connection Types**:
  - Display verbindet sich zur Lobby
  - Player verbindet sich zur Lobby mit Player-ID
- **Message Types**:
  - `HELLO`: Initial Handshake (Client-Typ, Session-Info)
  - `JOIN_LOBBY`: Spieler tritt Lobby bei
  - `PLAYER_ACTION`: Player sendet Eingabe (Move, Attack, etc.)
  - `GAME_STATE_UPDATE`: Server broadcastet Game-State
  - `GAME_EVENT`: Server notifiziert Events (Player joined, Score changed, etc.)
  - `READY`: Client bestätigt Bereitschaft
  - `DISCONNECT`: Client trennt sich ab
  - `PING/PONG`: Heartbeat für Verbindungsverwaltung

### 2.5 REST-API (für Setup/Management)
- `POST /api/lobbies` - Neue Lobby erstellen
- `GET /api/lobbies/{lobbyId}` - Lobby-Info abrufen
- `GET /api/lobbies` - Alle offenen Lobbies auflisten
- `DELETE /api/lobbies/{lobbyId}` - Lobby löschen (nur Host)
- `POST /api/lobbies/{lobbyId}/players/{playerId}/ready` - Spieler ready markieren
- `DELETE /api/lobbies/{lobbyId}/players/{playerId}` - Spieler aus Lobby entfernen

## 3. Datenmodelle

### 3.1 Lobby
```
{
  id: String (UUID),
  code: String (6-8 Zeichen, eindeutig, lesbar),
  hostId: String,
  displayClientId: String (optional - Display verbindet sich),
  status: OPEN | READY | IN_PROGRESS | FINISHED,
  players: Map<String, Player>,
  gameState: GameState,
  settings: GameSettings,
  createdAt: Timestamp,
  lastActivity: Timestamp
}
```

### 3.2 Player
```
{
  id: String (UUID),
  username: String,
  role: PLAYER | SPECTATOR | HOST,
  status: CONNECTED | READY | PLAYING | DISCONNECTED,
  sessionId: String (WebSocket Connection ID),
  joinedAt: Timestamp,
  character: Character (oder null für Spectator)
}
```

### 3.3 GameState
```
{
  lobbyId: String,
  status: SETUP | IN_PROGRESS | FINISHED,
  players: Map<String, PlayerGameState>,
  fieldElements: List<FieldElement>,
  currentPhase: int,
  elapsedTime: long,
  score: Map<String, int>,
  winner: String (optional),
  metadata: Map<String, Object>
}
```

### 3.4 PlayerGameState
```
{
  playerId: String,
  position: Position { x: float, y: float },
  velocity: Velocity { dx: float, dy: float },
  health: int,
  status: ACTIVE | DEAD | INACTIVE,
  lastUpdate: Timestamp
}
```

### 3.5 GameSettings
```
{
  minPlayers: int,
  maxPlayers: int,
  gameMode: String (z.B. "elimination", "score-based"),
  timeLimit: long (ms, oder 0 für unbegrenzt),
  fieldWidth: float,
  fieldHeight: float,
  customSettings: Map<String, Object>
}
```

## 4. Lobby-Lifecycle

```
┌─────────────┐
│    OPEN     │ ← Lobby wird erstellt, Players jointen
│ (Warteschl.)│
└──────┬──────┘
       │ (alle Spieler ready + min. Spieler erreicht)
       ↓
┌─────────────┐
│    READY    │ ← Host startet Spiel oder wartet weiter
└──────┬──────┘
       │ (Host klickt Start)
       ↓
┌─────────────┐
│ IN_PROGRESS │ ← Spiel läuft, Updates fließen
└──────┬──────┘
       │ (Endbedingung erreicht)
       ↓
┌─────────────┐
│  FINISHED   │ ← Spiel vorbei, Results anzeigen
└─────────────┘
```

## 5. WebSocket-Message Format

### 5.1 Client → Server

**Join Lobby**
```json
{
  "type": "JOIN_LOBBY",
  "lobbyCode": "ABC123",
  "clientType": "PLAYER|DISPLAY",
  "username": "PlayerName",
  "playerId": "uuid" (optional für Player-Reconnect)
}
```

**Player Action**
```json
{
  "type": "PLAYER_ACTION",
  "playerId": "uuid",
  "action": "MOVE|JUMP|ATTACK|...",
  "direction": "UP|DOWN|LEFT|RIGHT|...",
  "data": { /* action-spezifische Daten */ }
}
```

**Ready Signal**
```json
{
  "type": "READY",
  "playerId": "uuid"
}
```

### 5.2 Server → Client (Broadcast)

**Game State Update**
```json
{
  "type": "GAME_STATE_UPDATE",
  "gameState": {
    "status": "IN_PROGRESS",
    "players": { /* PlayerGameState Map */ },
    "fieldElements": [ /* Array */ ],
    "score": { /* Map */ },
    "timestamp": 1234567890
  }
}
```

**Game Event**
```json
{
  "type": "GAME_EVENT",
  "event": "PLAYER_JOINED|PLAYER_LEFT|PLAYER_DIED|SCORE_CHANGED|...",
  "playerId": "uuid",
  "data": { /* event-spezifische Daten */ }
}
```

**Lobby Status Update**
```json
{
  "type": "LOBBY_STATUS",
  "lobbyId": "uuid",
  "status": "OPEN|READY|IN_PROGRESS|FINISHED",
  "players": [ /* Player List */ ]
}
```

## 6. Sicherheit & Validierung

- **Session-Validierung**: Alle WebSocket-Messages müssen gültige Session enthalten
- **Berechtigungsprüfung**: Nur Host kann Lobby-Aktionen ausführen (Start, Close)
- **Input-Validierung**: Server validiert alle Player-Actions gegen Game-Rules
- **Rate Limiting**: Player-Actions haben maximale Frequenz (Anti-Spam)
- **Idempotenz**: Mehrfach-Sending derselben Action sollte keine doppelten Effekte haben
- **Connection Hijacking**: Sessions mit Token oder eindeutige identifiers schützen

## 7. Non-Funktionale Anforderungen

- **Latency**: <100ms zwischen Player-Action und Game-State Update
- **Scalability**: Minimum 100+ parallele Lobbies, 10+ Spieler pro Lobby
- **Reliability**: Graceful Handling bei Player-Disconnect/Reconnect
- **Memory**: Inaktive Lobbies nach 30+ Minuten aufräumen
- **Monitoring**: Logging aller kritischen Events

## 8. Fehlerbehandlung

- **Connection Lost**: Display/Player kann sich automatisch reconnecten mit SessionID
- **Lobby Full**: Neue Player erhalten Error wenn Lobby voll
- **Invalid Action**: Server sendet Error-Message, ignoriert Action
- **Timeout**: Player der >5min inaktiv ist wird entfernt
- **Server Error**: Graceful Error-Message zum Client, Lobby-State konsistent halten

## 9. Zukünftige Erweiterungen

- Chat zwischen Spielern
- Replays/Aufzeichnungen
- Matchmaking/Ranking
- In-Game Shop/Items
- Accounts & Persistence
- Mobile Web-UI
- Analytics & Telemetry
