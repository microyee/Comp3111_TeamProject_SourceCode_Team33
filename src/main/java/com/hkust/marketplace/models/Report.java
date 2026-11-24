package com.hkust.marketplace.models;

import java.time.LocalDateTime;

public record Report(String id, String reporter, Target target, String targetId, String subject,
                     String reason, LocalDateTime submittedAt, String resolution, LocalDateTime resolvedAt) {
    public enum Target { LISTING, RATING }

    public boolean isOpen() {
        return resolvedAt == null;
    }

    public Report resolve(String note, LocalDateTime at) {
        return new Report(id, reporter, target, targetId, subject, reason, submittedAt, note, at);
    }
}
