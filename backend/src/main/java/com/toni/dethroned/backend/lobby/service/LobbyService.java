package com.toni.dethroned.backend.lobby.service;

import com.toni.dethroned.backend.lobby.domain.GameSettings;
import com.toni.dethroned.backend.lobby.domain.Lobby;
import com.toni.dethroned.backend.lobby.domain.LobbyStatus;
import com.toni.dethroned.backend.lobby.domain.Player;
import com.toni.dethroned.backend.lobby.domain.PlayerRole;
import com.toni.dethroned.backend.lobby.domain.PlayerStatus;
import com.toni.dethroned.backend.lobby.exception.InvalidAdminException;
import com.toni.dethroned.backend.lobby.exception.LobbyFullException;
import com.toni.dethroned.backend.lobby.exception.LobbyNotFoundException;
import com.toni.dethroned.backend.lobby.exception.PlayerNotFoundException;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LobbyService {
    private final Map<String, Lobby> lobbies = new ConcurrentHashMap<>();
    private final Map<String, String> codeToLobbyId = new ConcurrentHashMap<>();

    public Lobby createLobby(String playerId, GameSettings settings) {
        String lobbyId = UUID.randomUUID().toString();
        String code = generateUniqueLobbyCode();

        Lobby lobby = new Lobby(lobbyId, code, playerId, settings);

        // Creator is automatically admin and member of the lobby
        Player creatorPlayer = new Player(playerId, "Admin", PlayerRole.PLAYER);
        lobby.addPlayer(creatorPlayer);

        lobbies.put(lobbyId, lobby);
        codeToLobbyId.put(code, lobbyId);

        return lobby;
    }

    public Lobby getLobbyById(String lobbyId) {
        Lobby lobby = lobbies.get(lobbyId);
        if (lobby == null) {
            throw new LobbyNotFoundException("Lobby not found: " + lobbyId);
        }
        return lobby;
    }

    public Lobby getLobbyByCode(String code) {
        String lobbyId = codeToLobbyId.get(code);
        if (lobbyId == null) {
            throw new LobbyNotFoundException("Lobby not found with code: " + code);
        }
        return getLobbyById(lobbyId);
    }

    public List<Lobby> getOpenLobbies() {
        return lobbies.values().stream()
                .filter(l -> l.getStatus() == LobbyStatus.OPEN)
                .toList();
    }

    public Collection<Lobby> getAllLobbies() {
        return lobbies.values();
    }

    public void deleteLobby(String lobbyId, String adminId) {
        Lobby lobby = getLobbyById(lobbyId);

        if (!lobby.getAdminId().equals(adminId)) {
            throw new InvalidAdminException("Only the admin can delete the lobby");
        }

        lobbies.remove(lobbyId);
        codeToLobbyId.remove(lobby.getCode());
    }

    private void deleteLobbyInternal(String lobbyId) {
        Lobby lobby = lobbies.get(lobbyId);
        if (lobby != null) {
            lobbies.remove(lobbyId);
            codeToLobbyId.remove(lobby.getCode());
        }
    }

    public Player addPlayer(String lobbyId, String playerId, String username, PlayerRole role) {
        Lobby lobby = getLobbyById(lobbyId);

        if (lobby.isFull()) {
            throw new LobbyFullException("Lobby is full");
        }

        Player player = new Player(playerId, username, role);
        lobby.addPlayer(player);

        return player;
    }

    public void playerLeaves(String lobbyId, String playerId) {
        Lobby lobby = getLobbyById(lobbyId);

        if (lobby.getPlayer(playerId) == null) {
            throw new PlayerNotFoundException("Player not found: " + playerId);
        }

        boolean wasAdmin = lobby.getAdminId().equals(playerId);

        lobby.removePlayer(playerId);

        // If last player: Delete lobby
        if (lobby.isEmpty()) {
            deleteLobbyInternal(lobbyId);
            return;
        }

        // If admin leaves: Transfer admin rights to oldest player
        if (wasAdmin) {
            lobby.transferAdminToOldestPlayer();
        }
    }

    public void markPlayerReady(String lobbyId, String playerId) {
        Lobby lobby = getLobbyById(lobbyId);
        Player player = lobby.getPlayer(playerId);

        if (player == null) {
            throw new PlayerNotFoundException("Player not found: " + playerId);
        }

        player.setStatus(PlayerStatus.READY);
        lobby.updateLastActivity();

        // Check if lobby can transition to READY (all players ready + min players met)
        if (lobby.allPlayersReady() && lobby.getPlayers().size() >= lobby.getSettings().getMinPlayers()
            && lobby.getStatus() == LobbyStatus.OPEN) {
            lobby.setStatus(LobbyStatus.READY);
        }
    }

    private String generateUniqueLobbyCode() {
        String code;
        do {
            code = RandomStringUtils.secure().nextAlphanumeric(6).toUpperCase();
        } while (codeToLobbyId.containsKey(code));
        return code;
    }
}
