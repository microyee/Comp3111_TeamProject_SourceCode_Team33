package com.hkust.marketplace.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A buyer's offer, borrow request or giveaway claim for one listing.
 *
 * <p>This is the UML {@code Request} entity: a Request belongs to one listing and may
 * create at most one {@link Deal}.  The admin take-down flow uses active requests to
 * decide which buyers must be notified and which deals must be cancelled.</p>
 */
public class Request {

    private String id;
    private String listingId;
    private String listingTitle;
    private String buyerUsername;
    private String sellerUsername;
    private ListingType type;
    private String status; // Pending, Countered, Accepted, Declined, Cancelled
    private Double offerPrice;
    private LocalDate borrowStart;
    private LocalDate borrowEnd;

    // Counteroffer terms (optional).
    private Double counterPrice;
    private LocalDate counterBorrowStart;
    private LocalDate counterBorrowEnd;

    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public Request() {
        this.createdAt = LocalDateTime.now();
        this.status = "Pending";
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

    public ListingType getType() { return type; }
    public void setType(ListingType type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getOfferPrice() { return offerPrice; }
    public void setOfferPrice(Double offerPrice) { this.offerPrice = offerPrice; }

    public LocalDate getBorrowStart() { return borrowStart; }
    public void setBorrowStart(LocalDate borrowStart) { this.borrowStart = borrowStart; }

    public LocalDate getBorrowEnd() { return borrowEnd; }
    public void setBorrowEnd(LocalDate borrowEnd) { this.borrowEnd = borrowEnd; }

    public Double getCounterPrice() { return counterPrice; }
    public void setCounterPrice(Double counterPrice) { this.counterPrice = counterPrice; }

    public LocalDate getCounterBorrowStart() { return counterBorrowStart; }
    public void setCounterBorrowStart(LocalDate counterBorrowStart) {
        this.counterBorrowStart = counterBorrowStart;
    }

    public LocalDate getCounterBorrowEnd() { return counterBorrowEnd; }
    public void setCounterBorrowEnd(LocalDate counterBorrowEnd) {
        this.counterBorrowEnd = counterBorrowEnd;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    /** An active request is pending or countered, per the proposal's workflow policy. */
    public boolean isActive() {
        return "Pending".equalsIgnoreCase(status) || "Countered".equalsIgnoreCase(status);
    }

    public String getTypeDisplay() {
        if (type == null) return "—";
        return switch (type) {
            case SELL -> "Sell";
            case LEND -> "Lend";
            case GIVEAWAY -> "Giveaway";
        };
    }
}
