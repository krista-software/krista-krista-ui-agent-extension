/*
 * Krista Ui Agent Extension Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.uiagent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UiAgentSessionManagerTest {

    private UiAgentSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new UiAgentSessionManager();
    }

    @Test
    void testCreateSession() {
        // Given
        String sessionId = "session-123";
        String executionId = "exec-456";
        Map<String, Object> metadata = Map.of("key", "value");

        // When
        UiAgentSessionManager.SessionInfo created = sessionManager.createSession(sessionId, executionId, metadata);

        // Then
        assertNotNull(created);
        assertEquals(sessionId, created.sessionId());
        assertEquals(executionId, created.executionId());
        assertEquals(UiAgentSessionManager.SessionState.ACTIVE, created.state());
        assertEquals(metadata, created.metadata());
        assertNotNull(created.createdAt());
    }

    @Test
    void testCreateSessionWithNullMetadata() {
        // When
        UiAgentSessionManager.SessionInfo created = sessionManager.createSession("session-1", "exec-1", null);

        // Then
        assertNotNull(created.metadata());
        assertTrue(created.metadata().isEmpty());
    }

    @Test
    void testGetSession() {
        // Given
        String sessionId = "session-123";
        sessionManager.createSession(sessionId, "exec-456", Map.of());

        // When
        Optional<UiAgentSessionManager.SessionInfo> found = sessionManager.getSession(sessionId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(sessionId, found.get().sessionId());
    }

    @Test
    void testGetNonExistentSession() {
        // When
        Optional<UiAgentSessionManager.SessionInfo> found = sessionManager.getSession("non-existent");

        // Then
        assertTrue(found.isEmpty());
    }

    @Test
    void testUpdateSessionState() {
        // Given
        String sessionId = "session-123";
        sessionManager.createSession(sessionId, "exec-456", Map.of());

        // When
        Optional<UiAgentSessionManager.SessionInfo> updated =
            sessionManager.updateSessionState(sessionId, UiAgentSessionManager.SessionState.COMPLETED);

        // Then
        assertTrue(updated.isPresent());
        assertEquals(UiAgentSessionManager.SessionState.COMPLETED, updated.get().state());
    }

    @Test
    void testUpdateNonExistentSession() {
        // When
        Optional<UiAgentSessionManager.SessionInfo> updated =
            sessionManager.updateSessionState("non-existent", UiAgentSessionManager.SessionState.COMPLETED);

        // Then
        assertTrue(updated.isEmpty());
    }

    @Test
    void testCloseSession() {
        // Given
        String sessionId = "session-123";
        sessionManager.createSession(sessionId, "exec-456", Map.of());

        // When
        Optional<UiAgentSessionManager.SessionInfo> closed = sessionManager.closeSession(sessionId);

        // Then
        assertTrue(closed.isPresent());
        assertEquals(sessionId, closed.get().sessionId());

        // Verify session is removed
        assertTrue(sessionManager.getSession(sessionId).isEmpty());
    }

    @Test
    void testCloseNonExistentSession() {
        // When
        Optional<UiAgentSessionManager.SessionInfo> closed = sessionManager.closeSession("non-existent");

        // Then
        assertTrue(closed.isEmpty());
    }

    @Test
    void testIsSessionActive() {
        // Given
        String sessionId = "session-123";
        sessionManager.createSession(sessionId, "exec-456", Map.of());

        // Then
        assertTrue(sessionManager.isSessionActive(sessionId));

        // When state changes
        sessionManager.updateSessionState(sessionId, UiAgentSessionManager.SessionState.COMPLETED);

        // Then
        assertFalse(sessionManager.isSessionActive(sessionId));
    }

    @Test
    void testIsSessionActiveForNonExistent() {
        // When/Then
        assertFalse(sessionManager.isSessionActive("non-existent"));
    }

    @Test
    void testGetActiveSessionCount() {
        // Given
        sessionManager.createSession("session-1", "exec-1", Map.of());
        sessionManager.createSession("session-2", "exec-2", Map.of());
        sessionManager.createSession("session-3", "exec-3", Map.of());

        // When
        sessionManager.updateSessionState("session-2", UiAgentSessionManager.SessionState.COMPLETED);

        // Then
        assertEquals(2, sessionManager.getActiveSessionCount());
    }

    @Test
    void testCleanupExpiredSessions() throws InterruptedException {
        // Given
        sessionManager.createSession("session-1", "exec-1", Map.of());
        Thread.sleep(100); // Wait 100ms
        sessionManager.createSession("session-2", "exec-2", Map.of());

        // When - cleanup sessions older than 50ms
        int removed = sessionManager.cleanupExpiredSessions(0); // 0 seconds = immediate expiry

        // Then
        assertEquals(2, removed);
        assertEquals(0, sessionManager.getSessionCount());
    }

    @Test
    void testClearAllSessions() {
        // Given
        sessionManager.createSession("session-1", "exec-1", Map.of());
        sessionManager.createSession("session-2", "exec-2", Map.of());
        sessionManager.createSession("session-3", "exec-3", Map.of());

        // When
        sessionManager.clearAllSessions();

        // Then
        assertEquals(0, sessionManager.getSessionCount());
    }

    @Test
    void testGetSessionCount() {
        // Given
        assertEquals(0, sessionManager.getSessionCount());

        // When
        sessionManager.createSession("session-1", "exec-1", Map.of());
        sessionManager.createSession("session-2", "exec-2", Map.of());

        // Then
        assertEquals(2, sessionManager.getSessionCount());
    }
}

