package com.hkust.marketplace.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Deal {
    private String id;
    private String requestId; // The Request that created this Deal (0..1)
    private String listingId;
    private String listingTitle;
    private String buyerUsername;
    private String sellerUsername;
    private ListingType dealType;
    private String status; // Pending, Accepted, Completed, Returned, Cancelled
    private Double offerPrice;
    private LocalDate borrowStart;
    private LocalDate borrowEnd;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private boolean rated;

    public Deal() {
        this.createdAt = LocalDateTime.now();
        this.status = "Pending";
        this.rated = false;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }

    public String getBuyerUsername() { return buyerUsername; }
    public void setBuyerUsername(String buyerUsername) { this.buyerUsername = buyerUsername; }

    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }

    public ListingType getDealType() { return dealType; }
    public void setDealType(ListingType dealType) { this.dealType = dealType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getOfferPrice() { return offerPrice; }
    public void setOfferPrice(Double offerPrice) { this.offerPrice = offerPrice; }

    public LocalDate getBorrowStart() { return borrowStart; }
    public void setBorrowStart(LocalDate borrowStart) { this.borrowStart = borrowStart; }

    public LocalDate getBorrowEnd() { return borrowEnd; }
    public void setBorrowEnd(LocalDate borrowEnd) { this.borrowEnd = borrowEnd; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public boolean isRated() { return rated; }
    public void setRated(boolean rated) { this.rated = rated; }

    /**
     * An offer/borrow request/claim that still ties the listing to a buyer.  Admin
     * moderation uses this to cancel exactly the deals affected by a take-down.
     */
    public boolean isActiveOffer() {
        return "Pending".equalsIgnoreCase(status) || "Accepted".equalsIgnoreCase(status);
    }

    /** Human-readable deal type used by the admin tables. */
    public String getTypeDisplay() {
        if (dealType == null) return "—";
        return switch (dealType) {
            case SELL -> "Sell";
            case LEND -> "Lend";
            case GIVEAWAY -> "Giveaway";
        };
    }
}
