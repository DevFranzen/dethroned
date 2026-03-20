package com.toni.dethroned.backend.websocket.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionGroupTest {
    private ConnectionGroup connectionGroup;
    private WebSocketSession mockSession1;
    private WebSocketSession mockSession2;

    @BeforeEach
    void setUp() {
        connectionGroup = new ConnectionGroup("test-group-123");
        mockSession1 = mock(WebSocketSession.class);
        mockSession2 = mock(WebSocketSession.class);
        when(mockSession1.isOpen()).thenReturn(true);
        when(mockSession2.isOpen()).thenReturn(true);
    }

    @Test
    void testAddConnection() {
        PlayerConnection connection = connectionGroup.addConnection("player1", mockSession1);

        assertNotNull(connection);
        assertEquals("player1", connection.getPlayerId());
        assertTrue(connectionGroup.isPlayerConnected("player1"));
    }

    @Test
    void testRemoveConnection() {
        connectionGroup.addConnection("player1", mockSession1);
        assertTrue(connectionGroup.isPlayerConnected("player1"));

        connectionGroup.removeConnection("player1");
        assertFalse(connectionGroup.isPlayerConnected("player1"));
    }

    @Test
    void testGetConnectionCount() {
        assertEquals(0, connectionGroup.getConnectionCount());

        connectionGroup.addConnection("player1", mockSession1);
        assertEquals(1, connectionGroup.getConnectionCount());

        connectionGroup.addConnection("player2", mockSession2);
        assertEquals(2, connectionGroup.getConnectionCount());

        connectionGroup.removeConnection("player1");
        assertEquals(1, connectionGroup.getConnectionCount());
    }

    @Test
    void testGetActiveConnections() {
        connectionGroup.addConnection("player1", mockSession1);
        connectionGroup.addConnection("player2", mockSession2);

        assertEquals(2, connectionGroup.getActiveConnections().size());
    }

    @Test
    void testIsEmpty() {
        assertTrue(connectionGroup.isEmpty());

        connectionGroup.addConnection("player1", mockSession1);
        assertFalse(connectionGroup.isEmpty());

        connectionGroup.removeConnection("player1");
        assertTrue(connectionGroup.isEmpty());
    }

    @Test
    void testGetGroupId() {
        assertEquals("test-group-123", connectionGroup.getGroupId());
    }

    @Test
    void testBroadcastAll() throws Exception {
        connectionGroup.addConnection("player1", mockSession1);
        connectionGroup.addConnection("player2", mockSession2);

        String testMessage = "{\"type\":\"TEST\",\"message\":\"Hello\"}";
        connectionGroup.broadcastAll(testMessage);

        verify(mockSession1, times(1)).sendMessage(any());
        verify(mockSession2, times(1)).sendMessage(any());
    }

    @Test
    void testBroadcastAllExclude() throws Exception {
        connectionGroup.addConnection("player1", mockSession1);
        connectionGroup.addConnection("player2", mockSession2);

        String testMessage = "{\"type\":\"TEST\"}";
        connectionGroup.broadcastAll(testMessage, "player1");

        verify(mockSession1, never()).sendMessage(any());
        verify(mockSession2, times(1)).sendMessage(any());
    }

    @Test
    void testSendTo() throws Exception {
        connectionGroup.addConnection("player1", mockSession1);
        connectionGroup.addConnection("player2", mockSession2);

        String testMessage = "{\"type\":\"DIRECT\"}";
        connectionGroup.sendTo("player1", testMessage);

        verify(mockSession1, times(1)).sendMessage(any());
        verify(mockSession2, never()).sendMessage(any());
    }

    @Test
    void testReconnect() {
        WebSocketSession newSession = mock(WebSocketSession.class);
        when(newSession.isOpen()).thenReturn(true);

        connectionGroup.addConnection("player1", mockSession1);
        assertSame(mockSession1, connectionGroup.getPlayerConnection("player1").getWebSocketSession());

        connectionGroup.addConnection("player1", newSession);
        assertSame(newSession, connectionGroup.getPlayerConnection("player1").getWebSocketSession());
    }
}
