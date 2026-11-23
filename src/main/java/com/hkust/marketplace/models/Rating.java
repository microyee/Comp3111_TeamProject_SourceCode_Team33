package com.hkust.marketplace.models;

import java.time.LocalDateTime;

/**
 * A buyer's rating/review of a seller after a completed deal.
 *
 * <p>This is the UML {@code Rating} entity.  It may be linked to the completed
 * {@link Deal}; historical seeded ratings without a known deal keep {@code dealId} null
 * (the UML multiplicity is {@code 0..1}).</p>
 */
public class Rating {

    private String id;
    private String dealId;
    private String listingTitle;
    private String sellerUsername;
    private String buyerUsername;
    private int rating; // 1-5
    private String comment;
    private String sellerReply;
    private boolean hidden; // admin can hide
    private LocalDateTime createdAt;

    public Rating() {
        this.createdAt = LocalDateTime.now();
        this.hidden = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDealId() { return dealId; }
    public void setDealId(String dealId) { this.dealId = dealId; }

    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }

    public String getBuyerUsername() { return buyerUsername; }
    public void setBuyerUsername(String buyerUsername) { this.buyerUsername = buyerUsername; }

    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getSellerReply() { return sellerReply; }
    public void setSellerReply(String sellerReply) { this.sellerReply = sellerReply; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
