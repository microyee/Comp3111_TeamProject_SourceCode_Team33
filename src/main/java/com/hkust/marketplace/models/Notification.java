package com.hkust.marketplace.models;

import java.time.LocalDateTime;

public class Notification {
    private String id;
    private String username; // recipient
    private String title;
    private String message;
    private String category; // Offer, Chat, System, Rating, Submission, Report, Account, etc.
    private LocalDateTime timestamp;
    private boolean read;

    /**
     * Optional navigation target for admin notifications.  {@code relatedScreen} is one
     * of REVIEW, LISTINGS, USERS, DEALS or RATINGS; {@code relatedId} identifies the
     * specific entity.  Both may be null for plain announcements.
     */
    private String relatedScreen;
    private String relatedId;

    public Notification() {
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }

    public Notification(String username, String title, String message, String category) {
        this(username, title, message, category, null, null);
    }

    public Notification(String username, String title, String message, String category,
                        String relatedScreen, String relatedId) {
        this();
        this.username = username;
        this.title = title;
        this.message = message;
        this.category = category;
        this.relatedScreen = relatedScreen;
        this.relatedId = relatedId;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getRelatedScreen() { return relatedScreen; }
    public void setRelatedScreen(String relatedScreen) { this.relatedScreen = relatedScreen; }

    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String relatedId) { this.relatedId = relatedId; }

    public boolean hasRelatedTarget() { return relatedScreen != null; }
}
