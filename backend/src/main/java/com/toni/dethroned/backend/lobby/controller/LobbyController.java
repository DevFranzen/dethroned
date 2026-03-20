package com.toni.dethroned.backend.lobby.controller;

import com.toni.dethroned.backend.lobby.domain.Lobby;
import com.toni.dethroned.backend.lobby.domain.Player;
import com.toni.dethroned.backend.lobby.dto.AddPlayerRequest;
import com.toni.dethroned.backend.lobby.dto.CreateLobbyRequest;
import com.toni.dethroned.backend.lobby.dto.LobbyResponse;
import com.toni.dethroned.backend.lobby.dto.PlayerResponse;
import com.toni.dethroned.backend.lobby.service.LobbyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {
    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @PostMapping
    public ResponseEntity<LobbyResponse> createLobby(@Valid @RequestBody CreateLobbyRequest request) {
        Lobby lobby = lobbyService.createLobby(request.getHostId(), request.getGameSettings());
        return ResponseEntity.status(HttpStatus.CREATED).body(new LobbyResponse(lobby));
    }

    @GetMapping
    public ResponseEntity<List<LobbyResponse>> getOpenLobbies() {
        List<LobbyResponse> lobbies = lobbyService.getOpenLobbies().stream()
                .map(LobbyResponse::new)
                .toList();
        return ResponseEntity.ok(lobbies);
    }

    @GetMapping("/{lobbyId}")
    public ResponseEntity<LobbyResponse> getLobby(@PathVariable String lobbyId) {
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        return ResponseEntity.ok(new LobbyResponse(lobby));
    }

    @DeleteMapping("/{lobbyId}")
    public ResponseEntity<Void> deleteLobby(
            @PathVariable String lobbyId,
            @RequestHeader("X-Host-Id") String hostId) {
        lobbyService.deleteLobby(lobbyId, hostId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{lobbyId}/players")
    public ResponseEntity<PlayerResponse> addPlayer(
            @PathVariable String lobbyId,
            @Valid @RequestBody AddPlayerRequest request) {
        Player player = lobbyService.addPlayer(lobbyId, request.getUsername(), request.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(new PlayerResponse(player));
    }

    @PostMapping("/{lobbyId}/players/{playerId}/ready")
    public ResponseEntity<Void> markPlayerReady(
            @PathVariable String lobbyId,
            @PathVariable String playerId) {
        lobbyService.markPlayerReady(lobbyId, playerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{lobbyId}/players/{playerId}")
    public ResponseEntity<Void> removePlayer(
            @PathVariable String lobbyId,
            @PathVariable String playerId) {
        lobbyService.removePlayer(lobbyId, playerId);
        return ResponseEntity.noContent().build();
    }
}
