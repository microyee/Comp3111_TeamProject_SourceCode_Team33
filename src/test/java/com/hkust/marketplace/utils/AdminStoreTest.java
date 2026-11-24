package com.hkust.marketplace.utils;

import com.hkust.marketplace.models.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.hkust.marketplace.models.ListingStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminStoreTest {
    @Test
    void adminModerationRetainsSharedRecordsAndCancelsPendingOffers() {
        DummyDataService data = new DummyDataService(true, LocalDateTime::now);
        var items = data.getAllListings().stream().toList();
        Listing charger = items.stream().filter(l -> l.getTitle().contains("Charger")).findFirst().orElseThrow();
        Listing fridge = items.stream().filter(l -> l.getTitle().equals("Mini Fridge")).findFirst().orElseThrow();
        assertEquals(-1, data.takeDownListing(charger.getId(), "", "admin"));
        assertEquals(-1, data.takeDownListing(null, "reason", "admin"));
        assertEquals(-1, data.takeDownListing(fridge.getId(), "reason", "admin"));
        assertFalse(data.approveListing(charger.getId(), "admin"));
        assertFalse(data.approveListing(null, "admin"));
        assertFalse(data.rejectListing(fridge.getId(), " ", "admin"));
        assertFalse(data.rejectListing("missing", "Some reason", "admin"));
        assertFalse(data.rejectListing(charger.getId(), "Some reason", "admin"));
        assertTrue(data.rejectListing(fridge.getId(), "  Image needs replacing. ", "admin"));
        assertEquals(REJECTED, fridge.getStatus());
        assertEquals("Image needs replacing.", fridge.getRejectionReason());
        assertTrue(data.getNotificationsFor("diana").stream().anyMatch(n -> n.getMessage().contains("Image needs replacing")));
        assertEquals(1, data.takeDownListing(charger.getId(), "  Incorrect price  ", "admin"));
        assertEquals(REMOVED, charger.getStatus());
        assertEquals("Incorrect price", charger.getRemovalReason());
        assertTrue(data.getActiveRequestsForListing(charger.getId()).isEmpty());
        assertEquals("Cancelled", data.getDealsForListing(charger.getId()).get(0).getStatus());
        assertTrue(data.getNotificationsFor("bob").stream().anyMatch(n -> n.getMessage().contains("Incorrect price")));
        assertEquals(2, data.getAuditLog().size());
        assertFalse(data.getAllChats().isEmpty());
        assertFalse(data.getAllRatingsForSeller("diana").isEmpty());
    }

    @Test
    void adminProtectsSeedAccountAndCanManageUserVisibility() {
        DummyDataService data = new DummyDataService(true, LocalDateTime::now);
        assertFalse(data.deactivateUser("admin", "admin"));
        assertFalse(data.deleteUser("admin", "admin"));
        assertFalse(data.deleteUser("unknown", "admin"));
        assertFalse(data.deactivateUser("unknown", "admin"));
        assertFalse(data.reactivateUser("unknown", "admin"));
        assertFalse(data.reactivateUser("charlie", "admin"));
        User seller = data.findUser("charlie");
        SessionManager.login(seller);
        assertTrue(SessionManager.isSeller());
        assertTrue(SessionManager.isSessionValid());
        assertTrue(data.deactivateUser("charlie", "admin"));
        assertFalse(SessionManager.isSessionValid());
        assertFalse(data.deactivateUser("charlie", "admin"));
        assertTrue(data.getLiveListings().stream().noneMatch(l -> seller.getUsername().equals(l.getSellerUsername())));
        assertTrue(data.reactivateUser("charlie", "admin"));
        assertTrue(data.getLiveListings().stream().anyMatch(l -> seller.getUsername().equals(l.getSellerUsername())));
        assertTrue(data.deleteUser("charlie", "admin"));
        assertFalse(data.deleteUser("charlie", "admin"));
        assertFalse(data.reactivateUser("charlie", "admin"));
        assertTrue(data.findUser("charlie").isDeleted());
        assertTrue(data.getAllUsersIncludingDeleted().contains(seller));
        assertFalse(data.getAllUsers().contains(seller));
        assertFalse(data.getActiveSellers().contains(seller));
        assertTrue(data.getListingsBySeller("charlie").stream().allMatch(l -> l.getStatus() == REMOVED));
        assertTrue(data.getUnreadCount("admin") > 0);
        data.markNotificationRead(data.getNotificationsFor("admin").get(0).getId());
        data.markNotificationRead("bad-id");
        data.markAllNotificationsRead("admin");
        assertEquals(0, data.getUnreadCount("admin"));
        assertTrue(data.getUnreadCount("diana") > 0);
    }
}
