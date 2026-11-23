package com.hkust.marketplace.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A private chat thread between one buyer and one seller about one listing.
 * This is the UML {@code Chat} entity; messages are its composed {@link Message}s.
 */
public class Chat {

    private String id;
    private String listingId;
    private String listingTitle;
    private String buyerUsername;
    private String sellerUsername;
    private LocalDateTime createdAt;
    private final List<Message> messages = new ArrayList<>();

    public Chat() {
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }

    public String getBuyerUsername() { return buyerUsername; }
    public void setBuyerUsername(String buyerUsername) { this.buyerUsername = buyerUsername; }

    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Message> getMessages() { return messages; }

    public void addMessage(Message message) {
        messages.add(message);
    }
}
