package com.toni.dethroned.backend.websocket.service;

import com.toni.dethroned.backend.websocket.domain.ConnectionGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketConnectionManagerTest {
    private WebSocketConnectionManager manager;
    private WebSocketSession mockSession;

    @BeforeEach
    void setUp() {
        manager = new WebSocketConnectionManager();
        mockSession = mock(WebSocketSession.class);
        when(mockSession.isOpen()).thenReturn(true);
    }

    @Test
    void testGetConnectionGroupCreatesIfNotExists() {
        ConnectionGroup group = manager.getConnectionGroup("group-1");

        assertNotNull(group);
        assertEquals("group-1", group.getGroupId());
    }

    @Test
    void testGetConnectionGroupReturnsSameInstance() {
        ConnectionGroup group1 = manager.getConnectionGroup("group-1");
        ConnectionGroup group2 = manager.getConnectionGroup("group-1");

        assertSame(group1, group2);
    }

    @Test
    void testCreateConnectionGroupCreatesNewGroup() {
        ConnectionGroup group = manager.createConnectionGroup("group-2");

        assertNotNull(group);
        assertEquals("group-2", group.getGroupId());
        assertTrue(manager.hasConnectionGroup("group-2"));
    }

    @Test
    void testCreateConnectionGroupReturnsSameInstanceIfExists() {
        ConnectionGroup group1 = manager.createConnectionGroup("group-3");
        ConnectionGroup groupSame = manager.createConnectionGroup("group-3");

        assertSame(group1, groupSame);
    }

    @Test
    void testRemoveConnectionGroup() {
        manager.createConnectionGroup("group-4");
        assertTrue(manager.hasConnectionGroup("group-4"));

        manager.removeConnectionGroup("group-4");

        assertFalse(manager.hasConnectionGroup("group-4"));
    }

    @Test
    void testRemoveConnectionGroupDoesNotThrowIfNotExists() {
        assertDoesNotThrow(() -> manager.removeConnectionGroup("nonexistent"));
    }

    @Test
    void testHasConnectionGroup() {
        assertFalse(manager.hasConnectionGroup("group-5"));

        manager.createConnectionGroup("group-5");

        assertTrue(manager.hasConnectionGroup("group-5"));
    }

    @Test
    void testIsPlayerConnected() {
        ConnectionGroup group = manager.getConnectionGroup("group-6");
        assertFalse(manager.isPlayerConnected("player-1", "group-6"));

        group.addConnection("player-1", mockSession);

        assertTrue(manager.isPlayerConnected("player-1", "group-6"));
    }

    @Test
    void testIsPlayerConnectedReturnsFalseIfGroupNotExists() {
        assertFalse(manager.isPlayerConnected("player-1", "nonexistent-group"));
    }

    @Test
    void testMultipleGroupsAreIndependent() {
        ConnectionGroup group1 = manager.getConnectionGroup("group-7");
        manager.getConnectionGroup("group-8");

        group1.addConnection("player-1", mockSession);

        assertTrue(manager.isPlayerConnected("player-1", "group-7"));
        assertFalse(manager.isPlayerConnected("player-1", "group-8"));
    }

    @Test
    void testRemovingGroupRemovesConnections() {
        ConnectionGroup group = manager.getConnectionGroup("group-9");
        group.addConnection("player-1", mockSession);

        assertTrue(manager.isPlayerConnected("player-1", "group-9"));

        manager.removeConnectionGroup("group-9");

        assertFalse(manager.isPlayerConnected("player-1", "group-9"));
    }

    @Test
    void testConnectionGroupManagementConcurrency() {
        // Create multiple groups concurrently
        ConnectionGroup group1 = manager.getConnectionGroup("concurrent-1");
        manager.getConnectionGroup("concurrent-2");
        manager.getConnectionGroup("concurrent-3");

        assertTrue(manager.hasConnectionGroup("concurrent-1"));
        assertTrue(manager.hasConnectionGroup("concurrent-2"));
        assertTrue(manager.hasConnectionGroup("concurrent-3"));

        // Verify independence
        group1.addConnection("player-1", mockSession);
        assertFalse(manager.isPlayerConnected("player-1", "concurrent-2"));
        assertFalse(manager.isPlayerConnected("player-1", "concurrent-3"));
    }
}
