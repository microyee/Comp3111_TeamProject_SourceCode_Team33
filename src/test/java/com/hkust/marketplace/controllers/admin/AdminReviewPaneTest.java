package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Listing;
import com.hkust.marketplace.models.ListingStatus;
import com.hkust.marketplace.models.ListingType;
import com.hkust.marketplace.models.Seller;
import com.hkust.marketplace.testsupport.FxTest;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SessionManager;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminReviewPaneTest {

    @BeforeAll
    static void startFx() {
        FxTest.ensureFx();
    }

    private static Parent cardWithLabel(Parent root, String text) {
        FxTest.layout(root);
        return root.lookupAll(".card").stream()
                .filter(Parent.class::isInstance)
                .map(Parent.class::cast)
                .filter(card -> FxTest.hasLabel(card, text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No card for " + text));
    }

    private static Listing pending(DummyDataService data, String title, ListingType type,
                                   Double price, Integer days, String seller, String description,
                                   List<String> photos) {
        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setDescription(description);
        listing.setType(type);
        listing.setPrice(price);
        listing.setLendingDays(days);
        listing.setCategory("Electronics");
        listing.setCondition("Used");
        listing.setMeetupLocation("Library");
        listing.setSellerUsername(seller);
        listing.setSellerFullName(data.findUser(seller) == null ? seller : data.findUser(seller).getFullName());
        listing.setStatus(ListingStatus.PENDING_REVIEW);
        listing.setPostedAt(LocalDateTime.now().minusHours(1));
        listing.setPhotoPaths(photos);
        data.addListing(listing);
        return listing;
    }

    @Test
    void approvesRejectsAndHandlesConcurrentChanges() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        data.registerUser(new Seller("review-seller", "Review Seller", "Password1"));
        data.registerUser(new Seller("review-inactive", "Inactive Seller", "Password1"));
        data.deactivateUser("review-inactive", "admin");
        SessionManager.login(data.findUser("admin"));
        String logo = "/images/hkust_logo.png";

        Listing sell = pending(data, "Review SELL item", ListingType.SELL, 15.0, null,
                "review-seller", "Great condition", List.of(logo, logo));
        Listing lend = pending(data, "Review LEND item", ListingType.LEND, null, 3,
                "review-seller", "  ", List.of());
        Listing giveaway = pending(data, "Review GIVEAWAY item", ListingType.GIVEAWAY, null, null,
                "review-seller", null, List.of());
        Listing noPrice = pending(data, "Review NO PRICE item", ListingType.SELL, null, null,
                "review-seller", "No price", List.of());
        Listing noDays = pending(data, "Review NO DAYS item", ListingType.LEND, null, null,
                "review-seller", "No days", List.of());
        Listing inactiveSeller = pending(data, "Review inactive seller item", ListingType.SELL,
                5.0, null, "review-inactive", "Seller is away", List.of());

        FxTest.run(() -> {
            AdminReviewPane pane = new AdminReviewPane(new AdminDashboardController());
            Stage stage = FxTest.stage(pane);
            assertTrue(FxTest.hasLabel(pane, "Review SELL item"));
            assertTrue(FxTest.hasLabel(pane, "No photos uploaded."));
            assertTrue(FxTest.hasLabel(pane, "Price not set"));
            assertTrue(FxTest.hasLabel(pane, "Duration not set"));
            assertTrue(FxTest.hasLabel(pane, "No description provided."));

            Parent sellCard = cardWithLabel(pane, "Review SELL item");
            FxTest.withDialogs(FxTest.button(sellCard, "Approve"),
                    dialog -> FxTest.button(dialog, "Cancel").fire());
            assertEquals(ListingStatus.PENDING_REVIEW, sell.getStatus());

            FxTest.withDialogs(FxTest.button(sellCard, "Approve"),
                    dialog -> FxTest.button(dialog, "OK").fire(),
                    dialog -> FxTest.button(dialog, "OK").fire());
            assertEquals(ListingStatus.LIVE, sell.getStatus());
            assertTrue(data.getNotificationsFor("review-seller").stream()
                    .anyMatch(n -> "Listing Approved".equals(n.getTitle())));

            Parent inactiveCard = cardWithLabel(pane, "Review inactive seller item");
            FxTest.withDialogs(FxTest.button(inactiveCard, "Approve"),
                    dialog -> FxTest.button(dialog, "OK").fire(),
                    dialog -> FxTest.button(dialog, "OK").fire());
            assertEquals(ListingStatus.LIVE, inactiveSeller.getStatus());
            assertFalse(data.catalog().isPublic(inactiveSeller));

            Parent lendCard = cardWithLabel(pane, "Review LEND item");
            FxTest.withDialogs(FxTest.button(lendCard, "Reject"), dialog -> {
                Button submit = FxTest.button(dialog, "Submit");
                assertTrue(submit.isDisabled());
                FxTest.button(dialog, "Cancel").fire();
            });
            assertEquals(ListingStatus.PENDING_REVIEW, lend.getStatus());

            lendCard = cardWithLabel(pane, "Review LEND item");
            FxTest.withDialogs(FxTest.button(lendCard, "Reject"), dialog -> {
                Button submit = FxTest.button(dialog, "Submit");
                assertTrue(submit.isDisabled());
                ((TextArea) dialog.lookup(".text-area")).setText("  Needs clearer photos  ");
                submit.fire();
            }, dialog -> FxTest.button(dialog, "OK").fire());
            assertEquals(ListingStatus.REJECTED, lend.getStatus());
            assertEquals("Needs clearer photos", lend.getRejectionReason());

            assertTrue(data.approveListing(giveaway.getId(), "admin"));
            Parent giveawayCard = cardWithLabel(pane, "Review GIVEAWAY item");
            FxTest.withDialogs(FxTest.button(giveawayCard, "Approve"),
                    dialog -> FxTest.button(dialog, "OK").fire(),
                    dialog -> FxTest.button(dialog, "OK").fire());
            assertEquals(ListingStatus.LIVE, giveaway.getStatus());

            Parent noDaysCard = cardWithLabel(pane, "Review NO DAYS item");
            data.approveListing(noDays.getId(), "admin");
            FxTest.withDialogs(FxTest.button(noDaysCard, "Approve"),
                    dialog -> FxTest.button(dialog, "OK").fire(),
                    dialog -> FxTest.button(dialog, "OK").fire());
            assertEquals(ListingStatus.LIVE, noDays.getStatus());

            Parent noPriceCard = cardWithLabel(pane, "Review NO PRICE item");
            FxTest.withDialogs(FxTest.button(noPriceCard, "Reject"), dialog -> {
                ((TextArea) dialog.lookup(".text-area")).setText("Reason");
                FxTest.button(dialog, "Submit").fire();
            }, dialog -> FxTest.button(dialog, "OK").fire());
            assertEquals(ListingStatus.REJECTED, noPrice.getStatus());

            stage.hide();
        });
        SessionManager.logout();
    }

    @Test
    void staleCardsAndCardClicksOpenWarningsAndTheEditDialog() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        data.registerUser(new Seller("review-edit-seller", "Edit Seller", "Password1"));
        SessionManager.login(data.findUser("admin"));
        Listing listing = pending(data, "Review stale item", ListingType.SELL, 7.0, null,
                "review-edit-seller", "Edit me", List.of());

        var paneRef = new java.util.concurrent.atomic.AtomicReference<AdminReviewPane>();
        var stageRef = new java.util.concurrent.atomic.AtomicReference<Stage>();
        FxTest.run(() -> {
            AdminReviewPane pane = new AdminReviewPane(new AdminDashboardController());
            Stage stage = FxTest.stage(pane);
            paneRef.set(pane);
            stageRef.set(stage);
            Parent card = cardWithLabel(pane, "Review stale item");
            data.removeListing(listing.getId());
            card.fireEvent(mouseClick());
        });
        FxTest.waitForDialog(8000);
        FxTest.run(() -> {
            var dialog = FxTest.visibleDialog();
            assertTrue(FxTest.hasLabel(dialog, "Listing no longer available"));
            FxTest.button(dialog, "OK").fire();
        });

        Listing second = pending(data, "Review editable item", ListingType.SELL, 9.0, null,
                "review-edit-seller", "Edit me too", List.of());
        FxTest.run(() -> {
            FxTest.button(paneRef.get(), "Refresh").fire();
            Parent editCard = cardWithLabel(paneRef.get(), "Review editable item");
            editCard.fireEvent(mouseClick());
        });
        FxTest.waitForDialog(8000);
        FxTest.run(() -> {
            var dialog = FxTest.visibleDialog();
            assertEquals("Edit listing: Review editable item", dialog.getHeaderText());
            FxTest.button(dialog, "Cancel").fire();
        });
        assertEquals(ListingStatus.PENDING_REVIEW, second.getStatus());
        FxTest.run(() -> stageRef.get().hide());
        SessionManager.logout();
    }

    private static MouseEvent mouseClick() {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, 10, 10, 10, 10, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false, null);
    }
}
