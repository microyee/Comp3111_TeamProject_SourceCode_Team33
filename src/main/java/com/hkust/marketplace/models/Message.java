package com.hkust.marketplace.models;

import java.time.LocalDateTime;

/** One free-text message inside a {@link Chat}. */
public class Message {

    private String id;
    private String senderUsername;
    private String content;
    private LocalDateTime sentAt;

    public Message() {
        this.sentAt = LocalDateTime.now();
    }

    public Message(String senderUsername, String content) {
        this();
        this.senderUsername = senderUsername;
        this.content = content;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
