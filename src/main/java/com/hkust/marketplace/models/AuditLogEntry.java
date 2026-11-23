package com.hkust.marketplace.models;

import java.time.LocalDateTime;

/**
 * Immutable audit-log record for an administrative action.
 *
 * <p>The project stores data in memory for the demo, so this record is not persisted.
 * In a production system it would be written to an append-only audit table.  The
 * important property is that destructive actions (deleting an account, taking down a
 * listing) leave a trace that survives the action itself.</p>
 */
public class AuditLogEntry {

    private final String actorUsername;
    private final String action;
    private final String targetType;
    private final String targetId;
    private final String details;
    private final LocalDateTime timestamp;

    public AuditLogEntry(String actorUsername, String action, String targetType,
                         String targetId, String details) {
        this.actorUsername = actorUsername;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public String getActorUsername() { return actorUsername; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
