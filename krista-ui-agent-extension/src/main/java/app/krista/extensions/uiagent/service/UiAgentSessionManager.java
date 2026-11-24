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

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages UI Agent session lifecycle and state tracking.
 * Thread-safe implementation for concurrent session operations.
 */
@Service
public class UiAgentSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(UiAgentSessionManager.class);

    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    /**
     * Session state enumeration
     */
    public enum SessionState {
        ACTIVE,
        COMPLETED,
        FAILED,
        CLOSED
    }

    /**
     * Session information record
     */
    public record SessionInfo(
        String sessionId,
        String executionId,
        Instant createdAt,
        SessionState state,
        Map<String, Object> metadata
    ) {
        public SessionInfo withState(SessionState newState) {
            return new SessionInfo(sessionId, executionId, createdAt, newState, metadata);
        }
    }

    /**
     * Create and track a new session
     *
     * @param sessionId UI Agent session ID
     * @param executionId Krista execution ID
     * @param metadata Additional session metadata
     * @return Created session info
     */
    public SessionInfo createSession(String sessionId, String executionId, Map<String, Object> metadata) {
        logger.info("Creating session: sessionId={}, executionId={}", sessionId, executionId);

        SessionInfo sessionInfo = new SessionInfo(
            sessionId,
            executionId,
            Instant.now(),
            SessionState.ACTIVE,
            metadata != null ? Map.copyOf(metadata) : Map.of()
        );

        sessions.put(sessionId, sessionInfo);
        logger.debug("Session created successfully: {}", sessionInfo);

        return sessionInfo;
    }

    /**
     * Get session information
     *
     * @param sessionId UI Agent session ID
     * @return Optional containing session info if found
     */
    public Optional<SessionInfo> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    /**
     * Update session state
     *
     * @param sessionId UI Agent session ID
     * @param newState New session state
     * @return Updated session info, or empty if session not found
     */
    public Optional<SessionInfo> updateSessionState(String sessionId, SessionState newState) {
        logger.info("Updating session state: sessionId={}, newState={}", sessionId, newState);

        SessionInfo updated = sessions.computeIfPresent(sessionId, (key, current) -> {
            SessionInfo newInfo = current.withState(newState);
            logger.debug("Session state updated: {} -> {}", current.state(), newState);
            return newInfo;
        });

        return Optional.ofNullable(updated);
    }

    /**
     * Close and remove a session
     *
     * @param sessionId UI Agent session ID
     * @return Removed session info, or empty if session not found
     */
    public Optional<SessionInfo> closeSession(String sessionId) {
        logger.info("Closing session: sessionId={}", sessionId);

        SessionInfo removed = sessions.remove(sessionId);
        if (removed != null) {
            logger.debug("Session closed and removed: {}", removed);
        } else {
            logger.warn("Attempted to close non-existent session: {}", sessionId);
        }

        return Optional.ofNullable(removed);
    }

    /**
     * Check if a session exists and is active
     *
     * @param sessionId UI Agent session ID
     * @return true if session exists and is in ACTIVE state
     */
    public boolean isSessionActive(String sessionId) {
        return getSession(sessionId)
            .map(info -> info.state() == SessionState.ACTIVE)
            .orElse(false);
    }

    /**
     * Get count of active sessions
     *
     * @return Number of sessions in ACTIVE state
     */
    public long getActiveSessionCount() {
        return sessions.values().stream()
            .filter(info -> info.state() == SessionState.ACTIVE)
            .count();
    }

    /**
     * Clean up expired sessions (older than specified duration)
     *
     * @param maxAgeSeconds Maximum age in seconds
     * @return Number of sessions cleaned up
     */
    public int cleanupExpiredSessions(long maxAgeSeconds) {
        logger.info("Cleaning up sessions older than {} seconds", maxAgeSeconds);

        Instant cutoff = Instant.now().minusSeconds(maxAgeSeconds);
        int removed = 0;

        for (Map.Entry<String, SessionInfo> entry : sessions.entrySet()) {
            SessionInfo info = entry.getValue();
            if (info.createdAt().isBefore(cutoff)) {
                sessions.remove(entry.getKey());
                removed++;
                logger.debug("Removed expired session: sessionId={}, age={}s",
                    info.sessionId(),
                    Instant.now().getEpochSecond() - info.createdAt().getEpochSecond());
            }
        }

        logger.info("Cleanup complete: {} sessions removed", removed);
        return removed;
    }

    /**
     * Clear all sessions (for shutdown/cleanup)
     */
    public void clearAllSessions() {
        logger.info("Clearing all sessions: count={}", sessions.size());
        sessions.clear();
    }

    /**
     * Get total session count
     *
     * @return Total number of tracked sessions
     */
    public int getSessionCount() {
        return sessions.size();
    }
}

