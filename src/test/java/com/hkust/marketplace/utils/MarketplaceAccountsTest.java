package com.hkust.marketplace.utils;

import com.hkust.marketplace.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.hkust.marketplace.models.UserRole.*;
import static org.junit.jupiter.api.Assertions.*;

class MarketplaceAccountsTest {
    DummyDataService data;
    MarketplaceAccounts accounts;

    @BeforeEach
    void setup() {
        data = new DummyDataService(false, LocalDateTime::now);
        accounts = new MarketplaceAccounts(data);
    }

    @Test
    void loginChecksRolePasswordAndAccountStatus() {
        User buyer = accounts.register(BUYER, "  ALICE ", "  Alice Chan  ", "Password1", "Password1", "Hall 1", null, null);
        assertInstanceOf(Buyer.class, buyer);
        assertEquals("alice", buyer.getUsername());
        assertEquals("Alice Chan", buyer.getFullName());
        assertEquals("Hall 1", buyer.getResidenceHall());
        assertTrue(PasswordUtils.isHashed(buyer.getPasswordHash()));
        assertSame(buyer, accounts.authenticate(BUYER, " Alice ", "Password1"));
        assertThrows(IllegalArgumentException.class, () -> accounts.authenticate(SELLER, "alice", "Password1"));
        assertThrows(IllegalArgumentException.class, () -> accounts.authenticate(BUYER, "alice", "wrong"));
        assertThrows(IllegalArgumentException.class, () -> accounts.authenticate(BUYER, "nobody", "Password1"));
        assertThrows(IllegalArgumentException.class, () -> accounts.authenticate(BUYER, "", "Password1"));
        assertThrows(IllegalArgumentException.class, () -> accounts.authenticate(BUYER, "alice", ""));
        assertThrows(IllegalArgumentException.class, () -> accounts.register(SELLER, "Alice", "Seller Alice", "Password1",
                "Password1", null, null, null));
        data.deactivateUser("alice", "admin");
        assertThrows(IllegalArgumentException.class, () -> accounts.authenticate(BUYER, "alice", "Password1"));
        assertTrue(data.deleteUser("alice", "admin"));
        assertThrows(IllegalArgumentException.class, () -> accounts.register(BUYER, "Alice", "Another Alice", "Password1",
                "Password1", null, null, null));
    }

    @Test
    void failedRegistrationDoesNotReserveUsername() {
        assertThrows(IllegalArgumentException.class, () -> accounts.register(ADMIN, "new-account", "Admin",
                "Password1", "Password1", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> accounts.register(BUYER, "a!", "Buyer", "Password1",
                "Password1", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> accounts.register(BUYER, "new-account", " ", "Password1",
                "Password1", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> accounts.register(BUYER, "new-account", "Buyer", "bad",
                "bad", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> accounts.register(BUYER, "new-account", "Buyer", "Password1",
                "Password2", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> accounts.register(BUYER, "new-account", "Buyer", "Password1",
                "Password1", "Unlisted hall", null, null));
        assertFalse(data.usernameExists("new-account"));
        assertInstanceOf(Buyer.class, accounts.register(BUYER, "new-account", "Buyer", "Password1",
                "Password1", null, null, null));
    }

    @Test
    void profileUpdateIsAtomicOnValidationFailureAndCanClearOptionalFields() {
        User buyer = accounts.register(BUYER, "alice", "Alice", "Password1", "Password1", "Hall 1", null, null);
        assertThrows(IllegalArgumentException.class, () -> accounts.updateProfile(buyer, "Changed", null, null, null,
                "wrong", "Password2", "Password2"));
        assertThrows(IllegalArgumentException.class, () -> accounts.updateProfile(buyer, "Changed", null, null, null,
                "Password1", "Password2", "Password3"));
        assertThrows(IllegalArgumentException.class, () -> accounts.updateProfile(buyer, "Changed", null, null, null,
                "", "", "Password3"));
        assertThrows(IllegalArgumentException.class, () -> accounts.updateProfile(null, "Changed", null, null, null,
                "", "", ""));
        assertThrows(IllegalArgumentException.class, () -> accounts.updateProfile(new Buyer("alice", "Imposter", "Password1"),
                "Changed", null, null, null, "", "", ""));
        assertEquals("Alice", buyer.getFullName());
        assertEquals("Hall 1", buyer.getResidenceHall());
        accounts.updateProfile(buyer, "Alice Chan", "Hall 2", null, null, "", "", "");
        assertEquals("Hall 2", buyer.getResidenceHall());
        accounts.updateProfile(buyer, "Alice", null, null, null, "Password1", "Password2", "Password2");
        assertNull(buyer.getResidenceHall());
        assertSame(buyer, accounts.authenticate(BUYER, "alice", "Password2"));
    }

    @Test
    void sellerDetailsValidateAndNameFeedsListingDisplay() {
        assertThrows(IllegalArgumentException.class, () -> accounts.register(SELLER, "diana", "Diana", "Password1",
                "Password1", null, "bio", "Not on campus"));
        assertThrows(IllegalArgumentException.class, () -> accounts.register(SELLER, "diana", "Diana", "Password1",
                "Password1", null, "b".repeat(501), null));
        User seller = accounts.register(SELLER, "diana", "Diana", "Password1", "Password1", null, " seller bio ", "Library");
        assertInstanceOf(Seller.class, seller);
        assertEquals("seller bio", seller.getBio());
        Listing listing = new Listing();
        listing.setSellerUsername("diana");
        data.addListing(listing);
        accounts.updateProfile(seller, "Diana Ng", null, "", null, "", "", "");
        assertNull(seller.getPreferredMeetup());
        assertEquals("", seller.getBio());
        assertEquals("Diana Ng", listing.getSellerFullName());
    }

    @Test
    void passwordStorageSupportsLegacyAccountsAndRejectsMalformedHashes() {
        String first = PasswordUtils.hash("Password1"), second = PasswordUtils.hash("Password1");
        assertNotEquals(first, second);
        assertTrue(PasswordUtils.verify("Password1", first));
        assertFalse(PasswordUtils.verify("Password2", first));
        assertFalse(PasswordUtils.verify("Password1", null));
        assertFalse(PasswordUtils.verify(null, first));
        assertFalse(PasswordUtils.verify("Password1", "pbkdf2$missing"));
        assertFalse(PasswordUtils.verify("Password1", "pbkdf2$bad$abc$def"));
        assertTrue(PasswordUtils.verify("legacy", "legacy"));
        assertFalse(PasswordUtils.verify("other", "legacy"));
        assertFalse(PasswordUtils.isHashed("legacy"));
        assertFalse(PasswordUtils.isHashed(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtils.hash(null));
        for (String weak : new String[] {null, "", "password1", "PASSWORD1", "Password"}) {
            assertNotEquals("OK", DummyDataService.checkPasswordStrength(weak));
        }
        assertEquals("OK", DummyDataService.checkPasswordStrength("Password1"));
    }
}
