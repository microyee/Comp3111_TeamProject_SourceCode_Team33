package com.hkust.marketplace.utils;

import com.hkust.marketplace.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.hkust.marketplace.models.ListingType.*;
import static com.hkust.marketplace.utils.MarketplaceCatalog.Sort.*;
import static org.junit.jupiter.api.Assertions.*;

class MarketplaceCatalogTest {
    final LocalDateTime now = LocalDateTime.of(2030, 7, 14, 12, 0);
    DummyDataService data;
    MarketplaceCatalog catalog;
    Buyer alice, bob;
    Seller seller;
    Admin admin;

    @BeforeEach
    void setup() {
        data = new DummyDataService(false, () -> now);
        catalog = data.catalog();
        alice = new Buyer("alice", "Alice", "Password1");
        bob = new Buyer("bob", "Bob", "Password1");
        seller = new Seller("seller", "Charlie Example", "Password1");
        admin = new Admin("admin", "Admin", "Password1");
        for (User user : List.of(alice, bob, seller, admin)) data.registerUser(user);
    }

    private Listing item(String title, ListingType type, Double price, int daysAgo) {
        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setDescription("Offered for collection");
        listing.setSellerUsername("seller");
        listing.setSellerFullName(seller.getFullName());
        listing.setPrice(price);
        listing.setType(type);
        listing.setCategory(type == SELL ? "Electronics" : "Others");
        listing.setCondition("Used");
        listing.setPostedAt(now.minusDays(daysAgo));
        listing.setStatus(ListingStatus.LIVE);
        data.addListing(listing);
        return listing;
    }

    @Test
    void combinesFiltersAndSortsUnpricedListingsLast() {
        Listing cheap = item("Cable", SELL, 15.0, 3);
        Listing expensive = item("Camera", SELL, 200.0, 2);
        Listing free = item("Mug", GIVEAWAY, null, 1);
        assertEquals(List.of(cheap, expensive), catalog.browse(alice, new MarketplaceCatalog.Filter(
                " COLLECTION ", SELL, "Electronics", "Used", 15.0, 200.0, PRICE_ASC)));
        assertEquals(List.of(expensive, cheap, free), catalog.browse(alice,
                new MarketplaceCatalog.Filter("CHARLIE", null, null, null, null, null, PRICE_DESC)));
        assertEquals(List.of(free, expensive, cheap), catalog.browse(alice,
                new MarketplaceCatalog.Filter("", null, null, null, null, null, NEWEST)));
        assertEquals(List.of(cheap), catalog.browse(alice, new MarketplaceCatalog.Filter("cable", null,
                null, null, null, 20.0, NEWEST)));
        assertEquals(List.of(expensive), catalog.browse(alice, new MarketplaceCatalog.Filter(null, null,
                null, null, 16.0, null, NEWEST)));
        assertTrue(catalog.browse(alice, new MarketplaceCatalog.Filter("no matches", null, null, null,
                null, null, NEWEST)).isEmpty());
        assertTrue(catalog.browse(alice, new MarketplaceCatalog.Filter("", null, "Furniture", null,
                null, null, NEWEST)).isEmpty());
        assertTrue(catalog.browse(alice, new MarketplaceCatalog.Filter("", null, null, "New",
                null, null, NEWEST)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> MarketplaceCatalog.validateRange(2.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> MarketplaceCatalog.validateRange(-1.0, null));
        assertThrows(IllegalArgumentException.class, () -> MarketplaceCatalog.validateRange(null, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> MarketplaceCatalog.validateRange(null, Double.POSITIVE_INFINITY));
        assertTrue(data.deactivateUser("seller", "admin"));
        assertTrue(catalog.browse(alice, new MarketplaceCatalog.Filter("", null, null, null,
                null, null, NEWEST)).isEmpty());
    }

    @Test
    void favouritesAndFollowsArePrivateAndApprovalNotifiesFollowers() {
        Listing listing = item("Cable", SELL, 15.0, 3);
        catalog.favourite(alice, listing.getId(), true);
        catalog.favourite(alice, listing.getId(), true);
        assertTrue(catalog.isFavourite(alice, listing.getId()));
        assertEquals(List.of(listing), catalog.favourites(alice));
        assertTrue(catalog.favourites(bob).isEmpty());
        catalog.follow(alice, " SELLER ", true);
        assertTrue(catalog.isFollowing(alice, "Seller"));
        assertFalse(catalog.isFollowing(bob, "seller"));
        assertEquals(List.of(seller), catalog.following(alice));
        assertThrows(IllegalArgumentException.class, () -> catalog.follow(alice, "missing", true));
        assertThrows(IllegalArgumentException.class, () -> catalog.follow(alice, "bob", true));
        listing.setStatus(ListingStatus.PENDING_REVIEW);
        assertThrows(IllegalArgumentException.class, () -> catalog.favourite(bob, listing.getId(), true));
        assertThrows(IllegalArgumentException.class, () -> catalog.favourite(bob, "missing", true));
        assertTrue(data.approveListing(listing.getId(), "admin"));
        assertEquals(1, data.getNotificationsFor("alice").size());
        assertTrue(data.getNotificationsFor("bob").isEmpty());
        catalog.follow(alice, "seller", false);
        assertTrue(catalog.following(alice).isEmpty());
        catalog.favourite(alice, listing.getId(), false);
        assertFalse(catalog.isFavourite(alice, listing.getId()));
    }

    @Test
    void listingDetailsAreVisibleToPriorParticipantsAndCountPublicViews() {
        Listing listing = item("Cable", SELL, 15.0, 3);
        assertThrows(IllegalArgumentException.class, () -> catalog.openListing(alice, "missing"));
        assertSame(listing, catalog.openListing(alice, listing.getId()));
        assertEquals(1, listing.getViewCount());
        data.activity().submit(alice, listing.getId(), MarketplaceActivity.Terms.sale(15));
        data.activity().openChat(bob, listing.getId());
        listing.setStatus(ListingStatus.REMOVED);
        assertSame(listing, catalog.openListing(alice, listing.getId()));
        assertSame(listing, catalog.openListing(bob, listing.getId()));
        assertEquals(1, listing.getViewCount());
        Listing privateListing = item("Cable 2", SELL, 20.0, 3);
        privateListing.setStatus(ListingStatus.REJECTED);
        assertThrows(IllegalArgumentException.class, () -> catalog.openListing(alice, privateListing.getId()));
        assertThrows(IllegalArgumentException.class, () -> catalog.openListing(seller, listing.getId()));
    }

    @Test
    void reportHasRequiredReasonAndModerationResolution() {
        Listing listing = item("Cable", SELL, 15.0, 3);
        assertThrows(IllegalArgumentException.class, () -> catalog.reportListing(alice, "missing", "reason"));
        assertThrows(IllegalArgumentException.class, () -> catalog.reportListing(alice, listing.getId(), "  "));
        Report report = catalog.reportListing(alice, listing.getId(), "  Mislabelled price  ");
        assertEquals("Mislabelled price", report.reason());
        assertEquals("REPORTS", data.getNotificationsFor("admin").get(0).getRelatedScreen());
        assertThrows(IllegalArgumentException.class, () -> catalog.reportListing(alice, listing.getId(), "Again"));
        assertThrows(IllegalArgumentException.class, () -> catalog.reports(bob));
        assertThrows(IllegalArgumentException.class, () -> catalog.resolve(admin, report.id(), "  "));
        assertThrows(IllegalArgumentException.class, () -> catalog.resolve(admin, "missing", "note"));
        assertEquals(ListingStatus.LIVE, listing.getStatus());
        catalog.resolve(admin, report.id(), "Seller corrected the title.");
        assertFalse(catalog.reports(admin).get(0).isOpen());
        assertEquals(1, data.getAuditLog().size());
        assertEquals("Report reviewed", data.getNotificationsFor("alice").get(0).getTitle());
        assertThrows(IllegalArgumentException.class, () -> catalog.resolve(admin, report.id(), "Again"));
        assertTrue(catalog.reportListing(alice, listing.getId(), "New problem").isOpen());
        listing.setStatus(ListingStatus.REMOVED);
        assertThrows(IllegalArgumentException.class, () -> catalog.reportListing(bob, listing.getId(), "Gone"));
    }
}
