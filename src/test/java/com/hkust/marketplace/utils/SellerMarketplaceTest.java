package com.hkust.marketplace.utils;

import com.hkust.marketplace.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.hkust.marketplace.models.ListingStatus.*;
import static com.hkust.marketplace.models.ListingType.*;
import static com.hkust.marketplace.utils.MarketplaceActivity.Decision.*;
import static com.hkust.marketplace.utils.MarketplaceActivity.Terms.*;
import static org.junit.jupiter.api.Assertions.*;

class SellerMarketplaceTest {
    private final LocalDateTime now = LocalDateTime.of(2030, 7, 14, 12, 0);
    private DummyDataService data;
    private SellerMarketplace marketplace;
    private MarketplaceActivity activity;
    private Seller seller, otherSeller;
    private Buyer buyer;
    private Admin admin;

    @BeforeEach
    void setup() {
        data = new DummyDataService(false, () -> now);
        activity = data.activity();
        marketplace = new SellerMarketplace(data);
        seller = new Seller("seller", "Seller", "Password1");
        otherSeller = new Seller("other", "Other seller", "Password1");
        buyer = new Buyer("buyer", "Buyer", "Password1");
        admin = new Admin("admin", "Admin", "Password1");
        for (User user : List.of(seller, otherSeller, buyer, admin)) data.registerUser(user);
    }

    private SellerMarketplace.ListingDetails details(ListingType type, String title, Double price, Integer days) {
        return new SellerMarketplace.ListingDetails(type, title, "Electronics", "Used", "Working device",
                "Library", price, days, List.of());
    }

    private Listing publish(ListingType type) {
        Listing listing = marketplace.save(seller, null, details(type, type.name() + " item", 19.99, 2));
        assertTrue(data.approveListing(listing.getId(), "admin"));
        return listing;
    }

    @Test
    void submitsEditsResubmitsAndDeletesWithModerationHistory() {
        var listing = marketplace.save(seller, null, details(SELL, "  Speakers  ", 79.99, 12));
        assertEquals(PENDING_REVIEW, listing.getStatus());
        assertEquals("Speakers", listing.getTitle());
        assertEquals("HK$ 79.99", listing.getPriceDisplay());
        assertNull(listing.getLendingDays());
        assertEquals("REVIEW", data.getNotificationsFor("admin").get(0).getRelatedScreen());
        assertTrue(data.getLiveListings().isEmpty());
        assertTrue(data.rejectListing(listing.getId(), "Show the power cable.", "admin"));
        assertEquals(REJECTED, listing.getStatus());
        assertEquals("Show the power cable.", listing.getRejectionReason());
        assertTrue(data.getNotificationsFor("seller").stream().anyMatch(n -> n.getMessage().contains("Show the power cable.")));
        var loan = marketplace.save(seller, listing.getId(), details(LEND, "Speakers with cable", 79.99, 4));
        assertSame(listing, loan);
        assertEquals(PENDING_REVIEW, loan.getStatus());
        assertEquals(4, loan.getLendingDays());
        assertNull(loan.getPrice());
        assertNull(loan.getRejectionReason());
        assertNull(loan.getReviewedAt());
        assertTrue(data.approveListing(loan.getId(), "admin"));
        assertTrue(data.getLiveListings().contains(loan));
        assertTrue(data.getNotificationsFor("seller").stream().anyMatch(n -> n.getTitle().equals("Listing Approved")));
        Chat thread = activity.openChat(buyer, loan.getId());
        marketplace.remove(seller, loan.getId());
        assertEquals(REMOVED, loan.getStatus());
        assertTrue(data.getLiveListings().isEmpty());
        assertSame(loan, data.catalog().openListing(buyer, loan.getId()));
        assertSame(thread, activity.openChat(buyer, loan.getId()));
        assertThrows(IllegalArgumentException.class, () -> marketplace.save(seller, loan.getId(),
                details(GIVEAWAY, "Speakers", null, null)));
    }

    @Test
    void rejectsInvalidListingDetailsWithoutChangingExistingData() {
        Listing item = publish(SELL);
        assertThrows(IllegalArgumentException.class, () -> marketplace.save(seller, item.getId(), null));
        assertThrows(IllegalArgumentException.class, () -> marketplace.save(seller, item.getId(),
                details(null, "Item", 10.0, null)));
        for (Double price : new Double[] {null, -1.0, Double.NaN, 20.111, 1_000_001.0}) {
            assertThrows(IllegalArgumentException.class, () -> marketplace.save(seller, item.getId(), details(SELL, "Item", price, null)));
        }
        for (Integer days : new Integer[] {null, 0, 366}) {
            assertThrows(IllegalArgumentException.class, () -> marketplace.save(seller, item.getId(), details(LEND, "Item", null, days)));
        }
        assertThrows(IllegalArgumentException.class, () -> marketplace.save(seller, item.getId(), details(GIVEAWAY, "", null, null)));
        for (var invalid : List.of(
                new SellerMarketplace.ListingDetails(GIVEAWAY, "Lamp", null, "Used", "Description", "Library", null, null, null),
                new SellerMarketplace.ListingDetails(GIVEAWAY, "Lamp", "Invalid", "Used", "Description", "Library", null, null, null),
                new SellerMarketplace.ListingDetails(GIVEAWAY, "Lamp", "Furniture", null, "Description", "Library", null, null, null),
                new SellerMarketplace.ListingDetails(GIVEAWAY, "Lamp", "Furniture", "Invalid", "Description", "Library", null, null, null),
                new SellerMarketplace.ListingDetails(GIVEAWAY, "Lamp", "Furniture", "Used", "", "Library", null, null, null),
                new SellerMarketplace.ListingDetails(GIVEAWAY, "Lamp", "Furniture", "Used", "Description", null, null, null, null),
                new SellerMarketplace.ListingDetails(GIVEAWAY, "Lamp", "Furniture", "Used", "Description", "Off-campus", null, null, null),
                new SellerMarketplace.ListingDetails(GIVEAWAY, "Lamp", "Furniture", "Used", "Description", "Library", null, null,
                        List.of("1", "2", "3", "4", "5", "6", "7")),
                new SellerMarketplace.ListingDetails(GIVEAWAY, "Lamp", "Furniture", "Used", "Description", "Library", null, null,
                        List.of("  ")))) {
            assertThrows(IllegalArgumentException.class, () -> marketplace.save(seller, item.getId(), invalid));
        }
        assertEquals("SELL item", item.getTitle());
        assertEquals(19.99, item.getPrice());
        assertEquals(LIVE, item.getStatus());
        assertEquals(1, data.getNotificationsFor("admin").size());
        var giveaway = marketplace.save(seller, null, new SellerMarketplace.ListingDetails(GIVEAWAY, "Lamp", "Furniture",
                "Used", "Description", "Library", 19.0, 10, List.of("photo", "photo")));
        assertNull(giveaway.getPrice());
        assertNull(giveaway.getLendingDays());
        assertEquals(List.of("photo"), giveaway.getPhotoPaths());
        assertNotNull(marketplace.save(seller, null, new SellerMarketplace.ListingDetails(GIVEAWAY, "Lamp", "Furniture",
                "Used", "Description", "Library", null, null, null)));
    }

    @Test
    void rechecksOwnershipAndActiveRequestsBeforeChangesOrManualSale() {
        Listing listing = publish(SELL);
        assertThrows(IllegalArgumentException.class, () -> marketplace.save(buyer, null, details(SELL, "Fake", 2.0, null)));
        assertThrows(IllegalArgumentException.class, () -> marketplace.remove(otherSeller, listing.getId()));
        assertThrows(IllegalArgumentException.class, () -> marketplace.markSold(otherSeller, listing.getId()));
        assertThrows(IllegalArgumentException.class, () -> marketplace.canEdit(seller, "missing"));
        Request request = activity.submit(buyer, listing.getId(), sale(10));
        assertFalse(marketplace.canEdit(seller, listing.getId()));
        assertFalse(marketplace.canMarkSold(seller, listing.getId()));
        assertThrows(IllegalArgumentException.class, () -> marketplace.remove(seller, listing.getId()));
        assertThrows(IllegalArgumentException.class, () -> marketplace.save(seller, listing.getId(), details(SELL, "New", 20.0, null)));
        assertThrows(IllegalArgumentException.class, () -> marketplace.markSold(seller, listing.getId()));
        activity.respond(seller, request.getId(), ACCEPT, null);
        assertFalse(marketplace.canEdit(seller, listing.getId()));
        assertTrue(marketplace.canMarkSold(seller, listing.getId()));
        marketplace.markSold(seller, listing.getId());
        assertEquals(SOLD, listing.getStatus());
        assertEquals("Completed", data.findDealForRequest(request.getId()).getStatus());
        assertFalse(marketplace.canMarkSold(seller, listing.getId()));
        Listing other = publish(SELL);
        marketplace.markSold(seller, other.getId());
        assertEquals(SOLD, other.getStatus());
        assertEquals(1, data.getAllDeals().size());
        assertFalse(marketplace.canMarkSold(seller, publish(LEND).getId()));
        assertTrue(data.deactivateUser("seller", "admin"));
        assertThrows(IllegalArgumentException.class, () -> marketplace.stats(seller));
    }

    @Test
    void statisticsTrackActualActivityWithoutCountingCounterofferTwice() {
        Listing saleListing = publish(SELL), loanListing = publish(LEND), giveawayListing = publish(GIVEAWAY);
        data.catalog().openListing(buyer, saleListing.getId());
        data.catalog().openListing(buyer, saleListing.getId());
        data.catalog().openListing(buyer, loanListing.getId());
        Request offer = activity.submit(buyer, saleListing.getId(), sale(3));
        activity.respond(seller, offer.getId(), COUNTER, sale(5));
        activity.respond(buyer, offer.getId(), ACCEPT, null);
        Deal sale = data.findDealForRequest(offer.getId());
        activity.finish(buyer, sale.getId());
        activity.rate(buyer, sale.getId(), 4, "");
        Request loan = activity.submit(buyer, loanListing.getId(), loan(now, now.plusSeconds(20)));
        activity.respond(seller, loan.getId(), ACCEPT, null);
        activity.finish(buyer, data.findDealForRequest(loan.getId()).getId());
        Request claim = activity.submit(buyer, giveawayListing.getId(), giveaway());
        activity.respond(seller, claim.getId(), ACCEPT, null);
        activity.finish(buyer, data.findDealForRequest(claim.getId()).getId());
        var stats = marketplace.stats(seller);
        assertEquals(1, stats.offers());
        assertEquals(1, stats.loanRequests());
        assertEquals(1, stats.claims());
        assertEquals(1, stats.completedSales());
        assertEquals(1, stats.returnedLoans());
        assertEquals(1, stats.completedGiveaways());
        assertEquals(3, stats.views());
        assertEquals(4, stats.averageRating());
        assertEquals(2, stats.listings().get(0).views());
        assertEquals(1, stats.listings().get(0).completedDeals());
        assertTrue(marketplace.stats(otherSeller).listings().isEmpty());
    }

    @Test
    void sellerCanReplyAndFlagVisibleOwnReviewWithAdminResolution() {
        Listing listing = publish(SELL);
        Request offer = activity.submit(buyer, listing.getId(), sale(3));
        activity.respond(seller, offer.getId(), ACCEPT, null);
        Deal sale = data.findDealForRequest(offer.getId());
        activity.finish(buyer, sale.getId());
        Rating review = activity.rate(buyer, sale.getId(), 4, "Works well");
        assertThrows(IllegalArgumentException.class, () -> marketplace.reply(seller, "missing", "Reply"));
        assertThrows(IllegalArgumentException.class, () -> marketplace.reply(otherSeller, review.getId(), "Reply"));
        assertThrows(IllegalArgumentException.class, () -> marketplace.reply(seller, review.getId(), "  "));
        assertThrows(IllegalArgumentException.class, () -> marketplace.flag(seller, review.getId(), ""));
        marketplace.reply(seller, review.getId(), "  Thanks! ");
        assertEquals("Thanks!", review.getSellerReply());
        assertNotNull(review.getSellerReplyAt());
        marketplace.reply(seller, review.getId(), "Thanks!");
        assertEquals(1, data.getNotificationsFor("buyer").stream().filter(n -> n.getTitle().contains("replied")).count());
        Report report = marketplace.flag(seller, review.getId(), "Check the meetup details");
        assertEquals(Report.Target.RATING, report.target());
        assertFalse(review.isHidden());
        assertThrows(IllegalArgumentException.class, () -> marketplace.flag(seller, review.getId(), "Duplicate"));
        review.setHidden(true);
        data.updateRating(review);
        assertEquals(0, data.getPublicRatingCount("seller"));
        assertEquals(1, data.getAllRatingsForSeller("seller").size());
        assertThrows(IllegalArgumentException.class, () -> marketplace.reply(seller, review.getId(), "Hidden"));
        assertThrows(IllegalArgumentException.class, () -> marketplace.flag(seller, review.getId(), "Hidden"));
        data.catalog().resolve(admin, report.id(), "Review hidden after checking the details.");
        assertFalse(data.catalog().reports(admin).get(0).isOpen());
    }
}
