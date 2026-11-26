package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Buyer;
import com.hkust.marketplace.models.Chat;
import com.hkust.marketplace.models.Deal;
import com.hkust.marketplace.models.Listing;
import com.hkust.marketplace.models.ListingStatus;
import com.hkust.marketplace.models.ListingType;
import com.hkust.marketplace.models.Rating;
import com.hkust.marketplace.models.Seller;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.testsupport.FxTest;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SessionManager;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AdminUsersPaneTest {

    @BeforeAll
    static void startFx() {
        FxTest.ensureFx();
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> comboWith(Parent root, String item) {
        return (ComboBox<String>) root.lookupAll(".combo-box").stream()
                .filter(ComboBox.class::isInstance)
                .map(ComboBox.class::cast)
                .filter(combo -> combo.getItems().contains(item))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No combo containing " + item));
    }

    private static Parent rowWith(Parent root, String label) {
        FxTest.layout(root);
        return root.lookupAll(".card").stream()
                .filter(Parent.class::isInstance)
                .map(Parent.class::cast)
                .filter(card -> FxTest.hasLabel(card, label))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No user row for " + label));
    }

    private static Listing liveListing(DummyDataService data, String seller, String title) {
        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setDescription("Coverage listing");
        listing.setSellerUsername(seller);
        listing.setSellerFullName(data.findUser(seller).getFullName());
        listing.setType(ListingType.SELL);
        listing.setCategory("Electronics");
        listing.setCondition("Used");
        listing.setMeetupLocation("Library");
        listing.setPrice(10.0);
        listing.setStatus(ListingStatus.LIVE);
        listing.setPostedAt(LocalDateTime.now());
        data.addListing(listing);
        return listing;
    }

    @Test
    void filtersRenderRowsAndCounts() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        SessionManager.login(data.findUser("admin"));
        User seller = new Seller("users-filter-seller", "Filter Seller", "Password1");
        User buyer = new Buyer("users-filter-buyer", "Filter Buyer", "Password1");
        User inactive = new Buyer("users-filter-inactive", "Filter Inactive", "Password1");
        User blankName = new Buyer("users-filter-blank", null, "Password1");
        for (User user : List.of(seller, buyer, inactive, blankName)) {
            data.registerUser(user);
        }
        data.deactivateUser(inactive.getUsername(), "admin");
        liveListing(data, seller.getUsername(), "Filter Seller Listing");

        FxTest.run(() -> {
            AdminUsersPane pane = new AdminUsersPane(new AdminDashboardController(), "users-filter");
            Stage stage = FxTest.stage(pane);
            TextField search = FxTest.field(pane, "Search username or full name...");
            assertEquals("users-filter", search.getText());

            assertTrue(FxTest.hasLabel(pane, "@users-filter-seller"));
            assertTrue(FxTest.hasLabel(pane, "(no name)"));

            search.setText("users-filter-seller");
            assertTrue(FxTest.hasLabel(pane, "@users-filter-seller"));
            assertFalse(FxTest.hasLabel(pane, "@users-filter-buyer"));

            search.setText("Filter Inactive");
            assertTrue(FxTest.hasLabel(pane, "@users-filter-inactive"));

            search.setText("no-such-user");
            assertTrue(FxTest.hasLabel(pane, "No matching users"));

            search.setText("users-filter");
            comboWith(pane, "Buyers").setValue("Buyers");
            assertFalse(FxTest.hasLabel(pane, "@users-filter-seller"));

            comboWith(pane, "Buyers").setValue("Sellers");
            assertTrue(FxTest.hasLabel(pane, "@users-filter-seller"));

            comboWith(pane, "Buyers").setValue("All Roles");
            comboWith(pane, "Active").setValue("Deactivated");
            assertTrue(FxTest.hasLabel(pane, "@users-filter-inactive"));
            assertFalse(FxTest.hasLabel(pane, "@users-filter-buyer"));

            comboWith(pane, "Active").setValue("Active");
            assertTrue(FxTest.hasLabel(pane, "@users-filter-buyer"));
            comboWith(pane, "Active").setValue("All Statuses");
            stage.hide();
        });
        SessionManager.logout();
    }

    @Test
    void editDeactivateReactivateAndDeleteFlows() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        SessionManager.login(data.findUser("admin"));
        User seller = new Seller("users-action-seller", "Action Seller", "Password1");
        User buyer = new Buyer("users-action-buyer", "Action Buyer", "Password1");
        data.registerUser(seller);
        data.registerUser(buyer);
        Listing listing = liveListing(data, seller.getUsername(), "Action Seller Listing");
        Rating rating = new Rating();
        rating.setSellerUsername(seller.getUsername());
        rating.setBuyerUsername(buyer.getUsername());
        rating.setRating(4);
        rating.setListingTitle("Action Seller Listing");
        rating.setComment("Good");
        data.addRating(rating);
        Chat chat = new Chat();
        chat.setBuyerUsername(buyer.getUsername());
        chat.setSellerUsername(seller.getUsername());
        data.addChat(chat);
        Deal deal = new Deal();
        deal.setBuyerUsername(buyer.getUsername());
        deal.setSellerUsername(seller.getUsername());
        data.addDeal(deal);

        FxTest.run(() -> {
            AdminUsersPane pane = new AdminUsersPane(new AdminDashboardController(), "users-action");
            Stage stage = FxTest.stage(pane);

            Parent sellerRow = rowWith(pane, "@users-action-seller");
            FxTest.withDialogs(FxTest.button(sellerRow, "Edit"), dialog -> {
                FxTest.anyTextField(dialog).setText("Action Seller Updated");
                FxTest.button(dialog, "Save Changes").fire();
            }, dialog -> FxTest.button(dialog, "OK").fire());
            assertEquals("Action Seller Updated", data.findUser("users-action-seller").getFullName());

            sellerRow = rowWith(pane, "@users-action-seller");
            FxTest.withDialogs(FxTest.button(sellerRow, "Deactivate"),
                    dialog -> FxTest.button(dialog, "Cancel").fire());
            assertTrue(data.findUser("users-action-seller").isActive());

            sellerRow = rowWith(pane, "@users-action-seller");
            FxTest.withDialogs(FxTest.button(sellerRow, "Deactivate"),
                    dialog -> {
                        assertTrue(FxTest.hasLabel(dialog, "Deactivate account"));
                        FxTest.button(dialog, "Deactivate").fire();
                    },
                    dialog -> FxTest.button(dialog, "OK").fire());
            assertFalse(data.findUser("users-action-seller").isActive());
            assertEquals(ListingStatus.LIVE, listing.getStatus());

            sellerRow = rowWith(pane, "@users-action-seller");
            FxTest.withDialogs(FxTest.button(sellerRow, "Reactivate"),
                    dialog -> FxTest.button(dialog, "Cancel").fire());
            assertFalse(data.findUser("users-action-seller").isActive());

            sellerRow = rowWith(pane, "@users-action-seller");
            FxTest.withDialogs(FxTest.button(sellerRow, "Reactivate"),
                    dialog -> FxTest.button(dialog, "Reactivate").fire(),
                    dialog -> FxTest.button(dialog, "OK").fire());
            assertTrue(data.findUser("users-action-seller").isActive());

            Parent buyerRow = rowWith(pane, "@users-action-buyer");
            FxTest.withDialogs(FxTest.button(buyerRow, "Delete"),
                    dialog -> FxTest.button(dialog, "Cancel").fire());
            assertFalse(data.findUser("users-action-buyer").isDeleted());

            buyerRow = rowWith(pane, "@users-action-buyer");
            FxTest.withDialogs(FxTest.button(buyerRow, "Delete"), dialog -> {
                assertTrue(FxTest.hasLabel(dialog, "Delete account"));
                FxTest.button(dialog, "Delete account").fire();
            }, dialog -> FxTest.button(dialog, "OK").fire());
            assertTrue(data.findUser("users-action-buyer").isDeleted());
            stage.hide();
        });
        SessionManager.logout();
    }

    @Test
    void addUserAuditLogAndEditingADeletedAccount() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        SessionManager.login(data.findUser("admin"));
        User vanish = new Buyer("users-vanish", "Vanish User", "Password1");
        data.registerUser(vanish);

        FxTest.run(() -> {
            AdminUsersPane pane = new AdminUsersPane(new AdminDashboardController(), "users-");
            Stage stage = FxTest.stage(pane);

            FxTest.withDialogs(FxTest.button(pane, "+ Add User"), dialog -> {
                FxTest.field(dialog, "e.g. Chan Tai Man").setText("Created By Admin");
                FxTest.field(dialog, "e.g. chan123").setText("users-created-buyer");
                ((PasswordField) dialog.lookup(".password-field")).setText("Password1");
                FxTest.button(dialog, "Create User").fire();
            }, dialog -> FxTest.button(dialog, "OK").fire());
            assertNotNull(data.findUser("users-created-buyer"));

            FxTest.withDialogs(FxTest.button(pane, "+ Add User"),
                    dialog -> FxTest.button(dialog, "Cancel").fire());

            FxTest.withDialogs(FxTest.button(pane, "Audit Log"),
                    dialog -> FxTest.button(dialog, "Close").fire());

            Parent vanishRow = rowWith(pane, "@users-vanish");
            FxTest.withDialogs(FxTest.button(vanishRow, "Edit"), dialog -> {
                data.deleteUser("users-vanish", "admin");
                FxTest.anyTextField(dialog).setText("Vanish Updated");
                FxTest.button(dialog, "Save Changes").fire();
            }, dialog -> {
                assertTrue(FxTest.hasLabel(dialog, "User no longer available"));
                FxTest.button(dialog, "OK").fire();
            });
            assertTrue(data.findUser("users-vanish").isDeleted());
            stage.hide();
        });
        SessionManager.logout();
    }
}
