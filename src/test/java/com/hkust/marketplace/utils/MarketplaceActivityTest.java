package com.hkust.marketplace.utils;

import com.hkust.marketplace.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static com.hkust.marketplace.models.ListingStatus.*;
import static com.hkust.marketplace.models.ListingType.*;
import static com.hkust.marketplace.utils.MarketplaceActivity.Decision.*;
import static com.hkust.marketplace.utils.MarketplaceActivity.Terms.*;
import static org.junit.jupiter.api.Assertions.*;

class MarketplaceActivityTest {
    LocalDateTime now;
    DummyDataService data;
    MarketplaceActivity activity;
    Buyer alice, bob;
    Seller seller, otherSeller;
    Admin admin;

    @BeforeEach
    void setup() {
        now = LocalDateTime.of(2030, 7, 14, 12, 0);
        data = new DummyDataService(false, () -> now);
        activity = data.activity();
        alice = new Buyer("alice", "Alice", "Password1");
        bob = new Buyer("bob", "Bob", "Password1");
        seller = new Seller("seller", "Seller", "Password1");
        otherSeller = new Seller("other", "Other seller", "Password1");
        admin = new Admin("admin", "Admin", "Password1");
        for (User user : List.of(alice, bob, seller, otherSeller, admin)) data.registerUser(user);
    }

    Listing listing(ListingType type) {
        Listing item = new Listing();
        item.setType(type);
        item.setTitle(type + " item");
        item.setCategory("Others");
        item.setSellerUsername("seller");
        item.setSellerFullName("Seller");
        if (type == SELL) item.setPrice(200.0);
        if (type == LEND) item.setLendingDays(2);
        item.setStatus(LIVE);
        data.addListing(item);
        return item;
    }

    @Test
    void counterofferHasOneDealAndClosesCompetingOffers() {
        Listing item = listing(SELL);
        Request first = activity.submit(alice, item.getId(), sale(150.25));
        Request competitor = activity.submit(bob, item.getId(), sale(155));
        assertEquals(LIVE, item.getStatus());
        activity.respond(seller, first.getId(), COUNTER, sale(170));
        assertEquals("Countered", first.getStatus());
        assertNull(data.findDealForRequest(first.getId()));
        assertEquals(150.25, first.getOfferPrice());
        assertEquals(170, first.getCounterPrice());
        assertThrows(IllegalArgumentException.class, () -> activity.respond(seller, first.getId(), ACCEPT, null));
        assertThrows(IllegalArgumentException.class, () -> activity.respond(bob, first.getId(), ACCEPT, null));
        assertThrows(IllegalArgumentException.class, () -> activity.respond(alice, first.getId(), COUNTER, sale(160)));
        activity.respond(alice, first.getId(), ACCEPT, null);
        Deal deal = data.findDealForRequest(first.getId());
        assertEquals(170, deal.getOfferPrice());
        assertEquals("Others", deal.getCategory());
        assertEquals("Declined", competitor.getStatus());
        assertEquals(RESERVED, item.getStatus());
        assertThrows(IllegalArgumentException.class, () -> activity.respond(alice, first.getId(), ACCEPT, null));
        assertThrows(IllegalArgumentException.class, () -> activity.finish(bob, deal.getId()));
        assertThrows(IllegalArgumentException.class, () -> activity.rate(alice, deal.getId(), 5, ""));
        activity.finish(alice, deal.getId());
        assertEquals(SOLD, item.getStatus());
        assertEquals("Completed", deal.getStatus());
        assertEquals(1, data.getAllDeals().size());
        assertEquals(1, activity.messages(alice, activity.chatForRequest(alice, first.getId()).getId(), "150.25").size());
        assertTrue(data.getNotificationsFor("bob").stream().anyMatch(n -> n.getTitle().contains("another request")));
    }

    @Test
    void withdrawnOrDeclinedRequestKeepsHistoryAndRespectsListingRemoval() {
        Listing item = listing(SELL);
        Request first = activity.submit(alice, item.getId(), sale(111));
        activity.respond(seller, first.getId(), COUNTER, sale(120));
        item.setStatus(REMOVED);
        assertThrows(IllegalArgumentException.class, () -> activity.respond(alice, first.getId(), ACCEPT, null));
        activity.respond(alice, first.getId(), DECLINE, null);
        assertEquals("Declined", first.getStatus());
        assertEquals(REMOVED, item.getStatus());
        item.setStatus(LIVE);
        Request second = activity.submit(alice, item.getId(), sale(110));
        activity.withdraw(alice, second.getId());
        assertEquals("Cancelled", second.getStatus());
        assertThrows(IllegalArgumentException.class, () -> activity.withdraw(alice, second.getId()));
        assertTrue(data.getAllDeals().isEmpty());
    }

    @Test
    void validatesInvalidDuplicateAndUnauthorizedOffersBeforeMutating() {
        Listing item = listing(SELL);
        for (Double amount : List.of(0.0, -2.0, Double.NaN, Double.POSITIVE_INFINITY, 4.001)) {
            assertThrows(IllegalArgumentException.class, () -> activity.submit(alice, item.getId(), sale(amount)));
        }
        for (var bad : List.of(giveaway(), new MarketplaceActivity.Terms(10.0, now, null),
                new MarketplaceActivity.Terms(10.0, null, now))) {
            assertThrows(IllegalArgumentException.class, () -> activity.submit(alice, item.getId(), bad));
        }
        assertThrows(IllegalArgumentException.class, () -> activity.submit(alice, item.getId(), null));
        assertThrows(IllegalArgumentException.class, () -> activity.submit(alice, "missing", sale(3)));
        assertThrows(IllegalArgumentException.class, () -> activity.submit(seller, item.getId(), sale(3)));
        assertThrows(IllegalArgumentException.class, () -> activity.respond(null, "missing", DECLINE, null));
        assertThrows(IllegalArgumentException.class, () -> activity.respond(seller, "missing", DECLINE, null));
        assertThrows(IllegalArgumentException.class, () -> activity.finish(admin, "missing"));
        assertThrows(IllegalArgumentException.class, () -> activity.finish(alice, "missing"));
        assertTrue(data.getAllRequests().isEmpty());
        Request request = activity.submit(alice, item.getId(), sale(3));
        assertThrows(IllegalArgumentException.class, () -> activity.submit(alice, item.getId(), sale(4)));
        assertThrows(IllegalArgumentException.class, () -> activity.respond(otherSeller, request.getId(), ACCEPT, null));
        assertThrows(IllegalArgumentException.class, () -> activity.respond(seller, request.getId(), COUNTER, sale(-4)));
        assertEquals("Pending", request.getStatus());
        assertTrue(data.getAllDeals().isEmpty());
        assertEquals(1, data.getNotificationsFor("seller").size());
    }

    @Test
    void giveawayHasOneConcurrentWinnerAndReleasesAfterDecline() throws Exception {
        Listing item = listing(GIVEAWAY);
        for (var bad : List.of(sale(9), loan(now, null), loan(null, now))) {
            assertThrows(IllegalArgumentException.class, () -> activity.submit(alice, item.getId(), bad));
        }
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Request>> calls = List.of(() -> claim(alice, item), () -> claim(bob, item));
            var results = executor.invokeAll(calls);
            Request first = results.get(0).get(), second = results.get(1).get();
            assertTrue((first == null) != (second == null));
            Request winner = first == null ? second : first;
            assertEquals(RESERVED, item.getStatus());
            assertThrows(IllegalArgumentException.class, () -> activity.respond(seller, winner.getId(), COUNTER, sale(4)));
            activity.respond(seller, winner.getId(), DECLINE, null);
            assertEquals(LIVE, item.getStatus());
            Request claim = activity.submit(alice, item.getId(), giveaway());
            activity.respond(seller, claim.getId(), ACCEPT, null);
            Deal deal = data.findDealForRequest(claim.getId());
            assertThrows(IllegalArgumentException.class, () -> activity.finish(seller, deal.getId()));
            activity.finish(alice, deal.getId());
            assertEquals(GIVEN, item.getStatus());
        } finally {
            executor.shutdownNow();
        }
    }

    private Request claim(User buyer, Listing item) {
        try { return activity.submit(buyer, item.getId(), giveaway()); }
        catch (IllegalArgumentException e) { return null; }
    }

    @Test
    void borrowingTransitionsAtExactTimesAndNotifiesOnlyOnce() {
        Listing item = listing(LEND);
        LocalDateTime start = now.plusSeconds(1), end = start.plusSeconds(5);
        Request request = activity.submit(alice, item.getId(), loan(start, end));
        activity.respond(seller, request.getId(), ACCEPT, null);
        Deal deal = data.findDealForRequest(request.getId());
        assertEquals(RESERVED, item.getStatus());
        assertFalse(activity.advanceLoans());
        assertThrows(IllegalArgumentException.class, () -> activity.finish(alice, deal.getId()));
        now = start;
        assertTrue(activity.advanceLoans());
        assertEquals(LENT, item.getStatus());
        now = start.plusSeconds(3);
        assertTrue(activity.advanceLoans());
        assertFalse(activity.advanceLoans());
        assertEquals(1, data.getNotificationsFor("alice").stream().filter(n -> "Reminder".equals(n.getCategory())).count());
        now = end;
        assertTrue(activity.advanceLoans());
        assertEquals(LIVE, item.getStatus());
        assertEquals("Auto-returned", deal.getStatus());
        assertEquals(end, deal.getCompletedAt());
        assertFalse(activity.advanceLoans());
        assertNotNull(activity.rate(alice, deal.getId(), 4, ""));
    }

    @Test
    void loanRevalidatesTimeAndCanBeReturnedEarlyWithoutRepublishingRemoval() {
        Listing item = listing(LEND);
        LocalDateTime start = now.plusSeconds(1);
        for (var bad : List.of(sale(5), loan(null, now), loan(start, null), loan(now.minusSeconds(1), now.plusHours(1)),
                loan(start, start.plusSeconds(4)), loan(start, start.plusDays(3)))) {
            assertThrows(IllegalArgumentException.class, () -> activity.submit(alice, item.getId(), bad));
        }
        item.setLendingDays(null);
        assertThrows(IllegalArgumentException.class, () -> activity.submit(alice, item.getId(), loan(start, start.plusDays(1))));
        item.setLendingDays(2);
        Request request = activity.submit(alice, item.getId(), loan(start, start.plusDays(1)));
        now = start.plusSeconds(1);
        assertThrows(IllegalArgumentException.class, () -> activity.respond(seller, request.getId(), ACCEPT, null));
        activity.respond(seller, request.getId(), COUNTER, loan(now, now.plusDays(2)));
        activity.respond(alice, request.getId(), ACCEPT, null);
        Deal deal = data.findDealForRequest(request.getId());
        assertEquals(LENT, item.getStatus());
        item.setStatus(REMOVED);
        activity.finish(alice, deal.getId());
        assertEquals("Returned", deal.getStatus());
        assertEquals(REMOVED, item.getStatus());
        assertThrows(IllegalArgumentException.class, () -> activity.finish(alice, deal.getId()));
    }

    @Test
    void deactivationReleasesClaimAndDeletionCancelsAcceptedSale() {
        Listing item = listing(GIVEAWAY);
        Request claim = activity.submit(alice, item.getId(), giveaway());
        assertTrue(data.deactivateUser("alice", "admin"));
        assertEquals("Cancelled", claim.getStatus());
        assertEquals(LIVE, item.getStatus());
        assertThrows(IllegalArgumentException.class, () -> activity.submit(alice, item.getId(), giveaway()));
        Listing sale = listing(SELL);
        Request offer = activity.submit(bob, sale.getId(), sale(20));
        activity.respond(seller, offer.getId(), ACCEPT, null);
        assertTrue(data.deleteUser("bob", "admin"));
        assertEquals("Cancelled", data.findDealForRequest(offer.getId()).getStatus());
        assertEquals("Cancelled", offer.getStatus());
        assertEquals(LIVE, sale.getStatus());
        assertTrue(data.reactivateUser("alice", "admin"));
    }

    @Test
    void startedLoanResolvesIfSellerIsDeleted() {
        Listing item = listing(LEND);
        Request request = activity.submit(alice, item.getId(), loan(now, now.plusSeconds(5)));
        activity.respond(seller, request.getId(), ACCEPT, null);
        Deal deal = data.findDealForRequest(request.getId());
        assertTrue(data.deleteUser(seller.getUsername(), "admin"));
        assertEquals("Accepted", deal.getStatus());
        now = now.plusSeconds(6);
        assertTrue(activity.advanceLoans());
        assertEquals("Auto-returned", deal.getStatus());
        assertEquals(REMOVED, item.getStatus());
    }

    @Test
    void privateChatRetainsHistoryAndRespectsUnavailableCounterparty() {
        Listing item = listing(SELL);
        Chat chat = activity.openChat(alice, item.getId());
        assertSame(chat, activity.openChat(alice, item.getId()));
        assertNotSame(chat, activity.openChat(bob, item.getId()));
        assertThrows(IllegalArgumentException.class, () -> activity.messages(admin, chat.getId(), ""));
        assertThrows(IllegalArgumentException.class, () -> activity.messages(null, chat.getId(), ""));
        assertThrows(IllegalArgumentException.class, () -> activity.send(alice, "missing", "Hi"));
        assertThrows(IllegalArgumentException.class, () -> activity.send(alice, chat.getId(), "  "));
        assertThrows(IllegalArgumentException.class, () -> activity.send(alice, chat.getId(), "a".repeat(2001)));
        assertThrows(IllegalArgumentException.class, () -> activity.chatForRequest(alice, "missing"));
        assertThrows(IllegalArgumentException.class, () -> activity.chats(admin));
        activity.send(alice, chat.getId(), "Meet at the Library?");
        activity.send(seller, chat.getId(), "Yes");
        assertEquals(1, activity.messages(seller, chat.getId(), "LIBRARY").size());
        assertEquals(1, activity.messages(seller, chat.getId(), "ALICE").size());
        assertTrue(activity.messages(alice, chat.getId(), "not there").isEmpty());
        assertEquals(1, activity.chats(alice).size());
        assertEquals(2, activity.chats(seller).size());
        Notification notice = data.getNotificationsFor("alice").get(0);
        assertThrows(IllegalArgumentException.class, () -> activity.readNotification(bob, notice.getId()));
        activity.readNotification(alice, notice.getId());
        assertTrue(notice.isRead());
        assertTrue(data.deactivateUser("seller", "admin"));
        assertSame(chat, activity.openChat(alice, item.getId()));
        assertEquals(2, activity.messages(alice, chat.getId(), "").size());
        assertThrows(IllegalArgumentException.class, () -> activity.send(alice, chat.getId(), "Still available?"));
        assertThrows(IllegalArgumentException.class, () -> activity.openChat(alice, "missing"));
    }

    @Test
    void ratingRequiresItsCompletedDealAndIsCountedOnceEvenWhenHidden() {
        Listing item = listing(SELL);
        Request request = activity.submit(alice, item.getId(), sale(20));
        activity.respond(seller, request.getId(), ACCEPT, null);
        Deal deal = data.findDealForRequest(request.getId());
        activity.finish(seller, deal.getId());
        assertThrows(IllegalArgumentException.class, () -> activity.rate(bob, deal.getId(), 5, ""));
        assertThrows(IllegalArgumentException.class, () -> activity.rate(alice, deal.getId(), 6, ""));
        assertThrows(IllegalArgumentException.class, () -> activity.rate(alice, deal.getId(), 0, ""));
        assertThrows(IllegalArgumentException.class, () -> activity.rate(alice, deal.getId(), 3, "a".repeat(1501)));
        Rating rating = activity.rate(alice, deal.getId(), 5, "  Good condition.  ");
        assertEquals("Good condition.", rating.getComment());
        assertEquals(5, data.getPublicAverageRating("seller"));
        rating.setHidden(true);
        data.updateRating(rating);
        assertEquals(0, data.getPublicRatingCount("seller"));
        assertThrows(IllegalArgumentException.class, () -> activity.rate(alice, deal.getId(), 3, ""));
    }
}
