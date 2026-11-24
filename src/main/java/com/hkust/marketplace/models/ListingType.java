package com.hkust.marketplace.models;

public enum ListingType {
    SELL("For Sale"),
    LEND("For Lending"),
    GIVEAWAY("Giveaway");

    private final String label;

    ListingType(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}
