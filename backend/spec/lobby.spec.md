# Lobby Workflow Spezifikation

## Übersicht

Der Lobby Workflow ist ein zustandsbasiertes System, das den gesamten Lebenszyklus einer Spiellobby vom Erstellen bis zum Löschen orchestriert. Der Workflow verwaltet mehrere Spieler, deren Rollen und Status, und automatisiert die Admin-Verwaltung sowie das Löschen verwaister Lobbies.

---

## 1. Authentifizierung & Player ID Generierung

### Schritt 1.1: Player authentifizieren
```
POST /api/auth/login
Content-Type: application/json
Body: {}
```

**Response:**
```json
{
  "playerId": "550e8400-e29b-41d4-a716-446655440001"
}
```

**Verhalten:**
- Jeder Aufruf generiert eine neue, eindeutige Player ID (UUID)
- Keine Authentifizierungsdaten erforderlich (Platzhalter für Phase 1)
- Player ID wird für alle nachfolgenden Operationen benötigt

---

## 2. Lobby erstellen

### Schritt 2.1: Lobby mit gültiger Player ID erstellen
```
POST /api/lobbies
Content-Type: application/json
Body: {
  "playerId": "550e8400-e29b-41d4-a716-446655440001",
  "gameSettings": {
    "minPlayers": 2,
    "maxPlayers": 4,
    "gameMode": "elimination",
    "timeLimit": 0,
    "fieldWidth": 800,
    "fieldHeight": 600
  }
}
```

**Response (201 Created):**
```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "code": "ABC123",
  "adminId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "OPEN",
  "players": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "username": "Admin",
      "role": "PLAYER",
      "status": "CONNECTED",
      "joinedAt": "2026-03-20T19:58:30.588865Z"
    }
  ],
  "settings": { ... },
  "createdAt": "2026-03-20T19:58:30.588865Z",
  "lastActivity": "2026-03-20T19:58:30.588865Z"
}
```

**Automatische Aktionen:**
- Creator wird automatisch als **Admin** eingesetzt (`adminId`)
- Creator wird automatisch als **Player** mit Status `CONNECTED` hinzugefügt
- Lobby Status ist `OPEN` (akzeptiert neue Player)
- Eindeutiger 6-stelliger Code wird generiert für Sharing

**Fehler:**
- `400`: Ungültige Game Settings
- `404`: Player ID nicht gültig

---

## 3. Spieler zur Lobby hinzufügen

### Voraussetzung: Spieler authentifizieren
Jeder Spieler muss sich zuerst am Authentifizierungs-Endpoint anmelden um seine Player ID zu erhalten (siehe Schritt 1.1).

### Schritt 3.1: Authentifizierter Spieler zu Lobby hinzufügen
```
POST /api/lobbies/{lobbyId}/players
Content-Type: application/json
Body: {
  "playerId": "550e8400-e29b-41d4-a716-446655440002",
  "username": "Alice",
  "role": "PLAYER"
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "username": "Alice",
  "role": "PLAYER",
  "status": "CONNECTED",
  "joinedAt": "2026-03-20T19:58:35.123456Z"
}
```

**Verhalten:**
- Die übergebene `playerId` wird verwendet (nicht neu generiert!)
- Player wird mit dieser ID zur Lobby hinzugefügt
- Status ist `CONNECTED` (nicht ready)
- `joinedAt` Timestamp wird gespeichert (wichtig für Admin-Transfer)
- Role kann `PLAYER` oder `SPECTATOR` sein

**Fehler:**
- `404`: Lobby nicht gefunden
- `409`: Lobby ist voll (`players.size() >= maxPlayers`)

**Rollen:**
- **PLAYER**: Aktiver Teilnehmer am Spiel, zählt zur `minPlayers` Anforderung
- **SPECTATOR**: Beobachter, zählt nicht zur `minPlayers` Anforderung

---

## 4. Spieler als bereit markieren

### Schritt 4.1: Einzelnen Spieler als bereit markieren
```
POST /api/lobbies/{lobbyId}/players/{playerId}/ready
Content-Type: application/json
```

**Response (200 OK):**
```
(kein Content-Body, nur Status 200)
```

**Verhalten:**
- Spieler Status ändert sich zu `READY`
- System prüft ob Lobby in `READY` Status übergehen kann:
  - ✅ Alle PLAYER (nicht Spectators) sind `READY`
  - ✅ Mindestanzahl Player (`minPlayers`) ist erreicht
- Falls beide Bedingungen erfüllt: Lobby Status → `READY`

**Fehler:**
- `404`: Lobby oder Spieler nicht gefunden

### Szenarien für Lobby Status Übergang zu READY

**Szenario A: 2 Player (Admin + 1 hinzugefügt), minPlayers=2**
1. Admin ist bereit → Lobby bleibt `OPEN`
2. Player 1 ist bereit → Lobby → `READY` (alle 2 bereit, minPlayers erfüllt)

**Szenario B: 1 Player + 1 Spectator, minPlayers=2**
1. Player ist bereit → Lobby bleibt `OPEN` (nur 1 PLAYER, braucht 2)
2. Spectator ist bereit → Lobby bleibt `OPEN` (Spectators zählen nicht)

**Szenario C: 3 Player, minPlayers=2**
1. Admin bereit → `OPEN`
2. Player 1 bereit → `READY` (mindestens 2 Player bereit)
3. Player 2 bereit → `READY`
4. Player 2 unready → `OPEN` (nicht alle bereit)
5. Player 2 bereit → `READY`

---

## 5. Spieler verlässt Lobby

### Schritt 5.1: Spieler verlässt Lobby
```
POST /api/lobbies/{lobbyId}/players/{playerId}/leave
Content-Type: application/json
Body: {}
```

**Response (204 No Content):**
```
(kein Content-Body)
```

**Automatische Aktionen bei Leave:**

#### Fall 1: Letzter Spieler verlässt → **Lobby wird gelöscht**
```
Lobby mit 1 Spieler:
- Spieler ruft /leave auf
- Lobby ist jetzt leer
- Lobby wird automatisch gelöscht
- Nachfolgende GET /api/lobbies/{lobbyId} → 404
```

#### Fall 2: Non-Admin Spieler verlässt → **Nichts weiteres**
```
Lobby mit 3 Spielern (Admin + 2 Player):
- Player 1 (nicht Admin) ruft /leave auf
- Player 1 wird entfernt
- Lobby hat noch 2 Spieler
- Admin bleibt Admin
- Lobby Status ändert sich nicht (automatisch)
```

#### Fall 3: Admin verlässt mit anderen Spielern → **Admin-Transfer**
```
Lobby mit 3 Spielern:
- Player 1 (Admin) - joinedAt: 10:00:00
- Player 2 - joinedAt: 10:00:05
- Player 3 - joinedAt: 10:00:10

Admin (Player 1) ruft /leave auf:
- Player 1 wird entfernt
- System findet "ältesten" verbleibenden Player (joinedAt)
- Player 2 wird neuer Admin (am längsten nach Admin in der Lobby)
- Lobby behält Status (OPEN/READY) bei
```

**Admin-Transfer Algorithmus:**
1. Spieler wird aus Lobby entfernt
2. Prüfung: War der Spieler Admin?
3. Falls ja: Finde Player mit kleinstem `joinedAt` Timestamp
4. Setze diesen als neuen `adminId`
5. Falls keine Player mehr: Lobby löschen

**Fehler:**
- `404`: Lobby oder Spieler nicht gefunden

---

## 6. Lobby manuell löschen

### Schritt 6.1: Admin löscht Lobby
```
DELETE /api/lobbies/{lobbyId}
X-Admin-Id: {playerId}
```

**Response (204 No Content):**
```
(kein Content-Body)
```

**Verhalten:**
- Admin Validierung via Header `X-Admin-Id`
- Lobby wird gelöscht
- Alle Spieler werden aus Lobby entfernt
- Nachfolgende GET /api/lobbies/{lobbyId} → 404

**Fehler:**
- `403`: Admin ID stimmt nicht mit aktuellem Admin überein
- `404`: Lobby nicht gefunden

---

## 7. Lobby Status Abrufen

### Schritt 7.1: Lobby Details abrufen
```
GET /api/lobbies/{lobbyId}
```

**Response (200 OK):**
```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "code": "ABC123",
  "adminId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "READY",
  "players": [ ... ],
  "settings": { ... },
  "createdAt": "2026-03-20T19:58:30.588865Z",
  "lastActivity": "2026-03-20T19:59:00.123456Z"
}
```

**Fehler:**
- `404`: Lobby nicht gefunden

---

## 8. Alle offenen Lobbies abrufen

### Schritt 8.1: Liste offener Lobbies
```
GET /api/lobbies
```

**Response (200 OK):**
```json
[
  {
    "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "code": "ABC123",
    "adminId": "550e8400-e29b-41d4-a716-446655440001",
    "status": "OPEN",
    "players": [ ... ],
    "settings": { ... },
    "createdAt": "2026-03-20T19:58:30.588865Z",
    "lastActivity": "2026-03-20T19:58:30.588865Z"
  },
  ...
]
```

**Verhalten:**
- Nur `OPEN` Lobbies werden zurückgegeben
- `READY`, `IN_PROGRESS`, `FINISHED` sind nicht enthalten

---

## Lobby Status Übergänge

```
┌─────────┐
│  OPEN   │ ← Lobby gerade erstellt
└────┬────┘
     │ Alle PLAYER bereit + minPlayers erfüllt
     ↓
┌─────────┐
│  READY  │ ← Bereit zum Starten
└────┬────┘
     │ Game startet
     ↓
┌──────────────┐
│ IN_PROGRESS  │ ← Spiel läuft
└────┬─────────┘
     │ Spiel endet
     ↓
┌──────────┐
│ FINISHED │ ← Spiel vorbei
└──────────┘
```

**Status Definitionen:**
- **OPEN**: Lobby akzeptiert neue Spieler, nicht alle bereit
- **READY**: Mindestanforderungen erfüllt, kann starten
- **IN_PROGRESS**: Spiel läuft
- **FINISHED**: Spiel beendet

---

## Player Status Übergänge

```
┌───────────┐
│ CONNECTED │ ← Player tritt bei
└─────┬─────┘
      │ markPlayerReady()
      ↓
┌────────┐
│ READY  │
└─────┬──┘
      │ Game startet
      ↓
┌─────────┐
│ PLAYING │
└────┬────┘
     │ Player disconnect/leave
     ↓
┌──────────────┐
│ DISCONNECTED │
└──────────────┘
```

---

## Vollständiger Workflow: Von Anfang bis Ende

### Beispiel: Zwei-Spieler Spiel

```
1. Player 1 authentifiziert sich
   POST /api/auth/login
   ← playerId1

2. Player 2 authentifiziert sich
   POST /api/auth/login
   ← playerId2

3. Player 1 erstellt Lobby
   POST /api/lobbies { playerId: playerId1, settings }
   ← Lobby { id, adminId: playerId1, status: OPEN, players: [Player1] }

4. Player 2 tritt Lobby bei mit seiner authentifizierten Player ID
   POST /api/lobbies/{id}/players { playerId: playerId2, username: "Alice", role: PLAYER }
   ← { id: playerId2, username: "Alice", ... }
   Lobby now has: [Player1, Player2]

5. Player 1 markiert sich bereit
   POST /api/lobbies/{id}/players/{playerId1}/ready
   Status bleibt OPEN (Player2 nicht bereit)

6. Player 2 markiert sich bereit
   POST /api/lobbies/{id}/players/{playerId2}/ready
   Status → READY (beide bereit, minPlayers erfüllt)

7. Spiel startet (by other system)
   Lobby Status → IN_PROGRESS

8. Player 1 verlässt während Spiel läuft
   POST /api/lobbies/{id}/players/{playerId1}/leave
   → playerId2 wird neuer Admin (ältester Player)
   → Lobby bleibt IN_PROGRESS (noch ein Player)

9. Player 2 verlässt
   POST /api/lobbies/{id}/players/{playerId2}/leave
   → Lobby ist leer
   → Lobby wird automatisch gelöscht

10. Verifikation: Lobby existiert nicht mehr
    GET /api/lobbies/{id}
    ← 404 Not Found
```

---

## Fehlerszenarien & Edge Cases

### Edge Case 1: Admin Transfer mit Spectators
```
Lobby:
- Player 1 (Admin) - PLAYER
- Player 2 - SPECTATOR
- Player 3 - PLAYER (joinedAt später)

Admin (Player 1) verlässt:
→ Player 2 ist älter als Player 3, aber ist SPECTATOR
→ Admin Transfer: Player 3 wird Admin (ältester PLAYER)
```

⚠️ **Aktuelle Implementierung**: Admin kann zu jedem Player transferieren, auch SPECTATORS. Dies könnte in Zukunft begrenzt werden auf nur PLAYER.

### Edge Case 2: Lobby voll
```
Lobby maxPlayers=2:
- Player 1 (Admin)
- Player 2

Versuch, Player 3 hinzuzufügen:
POST /api/lobbies/{id}/players { playerId: player3Id, username: "Bob", role: "PLAYER" }
← 409 Conflict - Lobby is full
```

### Edge Case 3: Ready Status Änderung
```
Alle 3 Player sind READY → Lobby READY
Player 2 wird unready:
→ Lobby Status bleibt READY (Implementierung sieht kein Rückgang vor)

Dieses Verhalten könnte je nach Geschäftslogik angepasst werden.
```

### Edge Case 4: Ungültige Admin ID beim Löschen
```
Lobby adminId = playerId1
DELETE /api/lobbies/{id}
X-Admin-Id: playerId2
← 403 Forbidden - InvalidAdminException
```

---

## Invarianten & Constraints

### Lobby Invarianten
- ✅ `adminId` ist immer einer der Players in der Lobby
- ✅ `players.size() <= maxPlayers`
- ✅ Nur PLAYER (nicht SPECTATOR) zählen zu minPlayers
- ✅ Lobby wird gelöscht wenn `players.isEmpty()`
- ✅ Admin wird automatisch transferiert wenn Admin verlässt und andere Player da sind

### Player Invarianten
- ✅ Jeder Player hat eindeutige ID
- ✅ `joinedAt` ist unveränderbar (sortierbar für Admin-Transfer)
- ✅ Status ist einer von: CONNECTED, READY, PLAYING, DISCONNECTED
- ✅ Role ist einer von: PLAYER, SPECTATOR

### Operationen
- ✅ Nur Admin kann Lobby löschen
- ✅ Admin kann sich selbst nicht "ready" markieren für Transaktionen (System entscheidet)
- ✅ Player können jederzeit leave aufrufen (auch wenn READY)

---

## Implementierungsdetails

### In-Memory Speicher
```java
// LobbyService verwendet ConcurrentHashMap
private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();

// Lobbies Indexierung
- Nach ID: O(1) Zugriff
- Nach Code: O(n) Suche (6-stelliger Code ist eindeutig)
- Nach Status (OPEN): O(n) Filtierung
```

### Thread Safety
- ✅ ConcurrentHashMap für Lobbies Map
- ✅ Players Map in Lobby ist auch ConcurrentHashMap
- ✅ Admin-Transfer und Remove sind atomar (innerhalb synchronized Block)

### Performance Überlegungen
- Admin Transfer: O(n) für `n = players.size()` (findet ältesten Player)
- Lobby löschen: O(1) (Direct Map removal)
- Lobby suchen: O(n) für Suche nach Code (könnte Index verwenden)

---

## Zukünftige Erweiterungen

### Phase 2 (geplant)
- [ ] WebSocket Integration für Real-Time Updates
- [ ] Persistent Database Speicherung
- [ ] Session Management und Authentifizierung
- [ ] Admin kann Players aus Lobby entfernen
- [ ] Ready Status Rückgang (Lobby → OPEN wenn nicht mehr alle bereit)

### Phase 3 (geplant)
- [ ] Game Loop Integration
- [ ] Score/Game State Tracking
- [ ] Player Statistics
- [ ] Lobby History & Archives

