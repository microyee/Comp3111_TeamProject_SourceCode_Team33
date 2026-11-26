package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Buyer;
import com.hkust.marketplace.models.Listing;
import com.hkust.marketplace.models.ListingStatus;
import com.hkust.marketplace.models.ListingType;
import com.hkust.marketplace.models.Seller;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.models.UserRole;
import com.hkust.marketplace.testsupport.FxTest;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.MarketplaceAccounts;
import com.hkust.marketplace.utils.MarketplaceActivity;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AdminListingFormDialogTest {

    @BeforeAll
    static void startFx() {
        FxTest.ensureFx();
    }

    private static String error(DialogPane pane) {
        Label label = (Label) pane.lookup(".error-text");
        return label == null ? null : label.getText();
    }

    private static Button saveButton(DialogPane pane, String text) {
        return FxTest.button(pane, text);
    }

    private static void choose(DialogPane pane, String prompt, Object value) {
        @SuppressWarnings("unchecked")
        ComboBox<Object> combo = (ComboBox<Object>) FxTest.combo(pane, prompt);
        combo.setValue(value);
    }

    private static void fillRequired(DialogPane pane, String title) {
        FxTest.field(pane, "Item name *").setText(title);
        choose(pane, "Category *", "Electronics");
        choose(pane, "Condition *", "Used");
        choose(pane, "Meet-up location *", "Library");
        ((TextArea) pane.lookup(".text-area")).setText("A detailed description for the listing.");
    }

    @Test
    void addSellListingValidatesEveryFieldBeforeSaving() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        MarketplaceAccounts accounts = new MarketplaceAccounts(data);
        User buyer = accounts.register(UserRole.BUYER, "form-buyer", "Form Buyer", "Password1", "Password1", null, null, null);
        User inactive = accounts.register(UserRole.SELLER, "form-inactive-seller", "Inactive Seller", "Password1", "Password1", null, "", "Library");
        data.deactivateUser(inactive.getUsername(), "admin");
        User seller = accounts.register(UserRole.SELLER, "form-seller", "Form Seller", "Password1", "Password1", null, "", "Library");

        AtomicReference<Optional<Listing>> result = new AtomicReference<>();
        FxTest.run(() -> {
            AdminListingFormDialog dialog = new AdminListingFormDialog(null, null,
                    List.of(buyer, inactive, seller));
            FxTest.withDialog(() -> result.set(dialog.showAndWait()), pane -> {
                Button save = saveButton(pane, "Create Listing");

                save.fire();
                assertEquals("Choose a listing type.", error(pane));

                choose(pane, "Listing type *", "Sell");
                save.fire();
                assertEquals("Item name is required.", error(pane));

                FxTest.field(pane, "Item name *").setText("x".repeat(101));
                save.fire();
                assertEquals("Item name must be 100 characters or fewer.", error(pane));

                FxTest.field(pane, "Item name *").setText("Coverage lamp");
                save.fire();
                assertEquals("Choose a category.", error(pane));

                choose(pane, "Category *", "Furniture");
                save.fire();
                assertEquals("Choose a condition.", error(pane));

                choose(pane, "Condition *", "Used");
                save.fire();
                assertEquals("Choose a meet-up location.", error(pane));

                choose(pane, "Meet-up location *", "Library");
                save.fire();
                assertEquals("Description is required.", error(pane));

                ((TextArea) pane.lookup(".text-area")).setText("   ");
                save.fire();
                assertEquals("Description is required.", error(pane));

                ((TextArea) pane.lookup(".text-area")).setText("y".repeat(2001));
                save.fire();
                assertEquals("Description must be 2000 characters or fewer.", error(pane));

                ((TextArea) pane.lookup(".text-area")).setText("A sturdy reading lamp.");
                save.fire();
                assertEquals("Choose the seller this listing is created for.", error(pane));

                choose(pane, "Choose a seller *", buyer);
                save.fire();
                assertEquals("The selected seller no longer exists or is not active.", error(pane));

                choose(pane, "Choose a seller *", inactive);
                save.fire();
                assertEquals("The selected seller no longer exists or is not active.", error(pane));

                choose(pane, "Choose a seller *", seller);
                FxTest.field(pane, "Price in HK$ (required for Sell)").setText("not-a-price");
                save.fire();
                assertEquals("A valid price is required for Sell listings.", error(pane));

                FxTest.field(pane, "Price in HK$ (required for Sell)").setText("0");
                save.fire();
                assertEquals("Price must be greater than 0.", error(pane));

                FxTest.field(pane, "Price in HK$ (required for Sell)").setText("1000001");
                save.fire();
                assertEquals("Price looks too large (maximum HK$1,000,000).", error(pane));

                FxTest.field(pane, "Price in HK$ (required for Sell)").setText("88.50");
                save.fire();
            });
        });
        Listing created = result.get().orElseThrow();
        assertEquals(ListingType.SELL, created.getType());
        assertEquals("Coverage lamp", created.getTitle());
        assertEquals(88.50, created.getPrice());
        assertEquals(ListingStatus.PENDING_REVIEW, created.getStatus());
        assertEquals("form-seller", created.getSellerUsername());
        assertNotNull(created.getPostedAt());
        assertNull(created.getLendingDays());
    }

    @Test
    void addLendAndGiveawayListingsUseTypeSpecificFields() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        MarketplaceAccounts accounts = new MarketplaceAccounts(data);
        User seller = accounts.register(UserRole.SELLER, "form-lend-seller", "Lend Seller", "Password1", "Password1", null, "", "Library");

        AtomicReference<Optional<Listing>> lendResult = new AtomicReference<>();
        FxTest.run(() -> {
            AdminListingFormDialog dialog = new AdminListingFormDialog(null, null, List.of(seller));
            FxTest.withDialog(() -> lendResult.set(dialog.showAndWait()), pane -> {
                choose(pane, "Listing type *", "Lend");
                fillRequired(pane, "Coverage drill");
                choose(pane, "Choose a seller *", seller);
                Button save = saveButton(pane, "Create Listing");

                FxTest.field(pane, "Lending duration in days (required for Lend, 1–365)").setText("many");
                save.fire();
                assertEquals("A valid lending duration is required for Lend listings.", error(pane));

                FxTest.field(pane, "Lending duration in days (required for Lend, 1–365)").setText("0");
                save.fire();
                assertEquals("Lending duration must be between 1 and 365 days.", error(pane));

                FxTest.field(pane, "Lending duration in days (required for Lend, 1–365)").setText("366");
                save.fire();
                assertEquals("Lending duration must be between 1 and 365 days.", error(pane));

                FxTest.field(pane, "Lending duration in days (required for Lend, 1–365)").setText("7");
                save.fire();
            });
        });
        Listing lend = lendResult.get().orElseThrow();
        assertEquals(ListingType.LEND, lend.getType());
        assertEquals(7, lend.getLendingDays());
        assertNull(lend.getPrice());

        AtomicReference<Optional<Listing>> giveawayResult = new AtomicReference<>();
        FxTest.run(() -> {
            AdminListingFormDialog dialog = new AdminListingFormDialog(null, null, null);
            FxTest.withDialog(() -> giveawayResult.set(dialog.showAndWait()), pane -> {
                choose(pane, "Listing type *", "Give Away");
                fillRequired(pane, "Coverage mug");
                choose(pane, "Choose a seller *", seller);
                saveButton(pane, "Create Listing").fire();
            });
        });
        Listing giveaway = giveawayResult.get().orElseThrow();
        assertEquals(ListingType.GIVEAWAY, giveaway.getType());
        assertNull(giveaway.getPrice());
        assertNull(giveaway.getLendingDays());
    }

    @Test
    void editModePrefillsFieldsLocksTheTypeAndSupportsPhotoRemoval() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        MarketplaceAccounts accounts = new MarketplaceAccounts(data);
        User seller = accounts.register(UserRole.SELLER, "form-edit-seller", "Edit Seller", "Password1", "Password1", null, "", "Library");
        User buyer = accounts.register(UserRole.BUYER, "form-edit-buyer", "Edit Buyer", "Password1", "Password1", null, null, null);

        Listing existing = new Listing();
        existing.setSellerUsername(seller.getUsername());
        existing.setSellerFullName(seller.getFullName());
        existing.setTitle("Editable lamp");
        existing.setType(ListingType.SELL);
        existing.setCategory("Furniture");
        existing.setCondition("Used");
        existing.setDescription("Lamp in good condition.");
        existing.setMeetupLocation("Library");
        existing.setPrice(25.0);
        existing.setStatus(ListingStatus.LIVE);
        existing.setPostedAt(LocalDateTime.now().minusDays(2));
        existing.setPhotoPaths(new java.util.ArrayList<>(List.of(
                "/images/hkust_logo.png", "/images/hkust_logo.png")));
        data.addListing(existing);
        data.activity().submit(buyer, existing.getId(),
                new MarketplaceActivity.Terms(10.0, null, null));

        AtomicReference<Optional<Listing>> result = new AtomicReference<>();
        FxTest.run(() -> {
            AdminListingFormDialog dialog = new AdminListingFormDialog(null, existing, null);
            FxTest.withDialog(() -> result.set(dialog.showAndWait()), pane -> {
                ComboBox<?> type = FxTest.combo(pane, "Listing type *");
                assertTrue(type.isDisabled());
                assertNotNull(type.getTooltip());

                choose(pane, "Listing type *", "Lend");
                FxTest.field(pane, "Lending duration in days (required for Lend, 1–365)").setText("5");
                saveButton(pane, "Save Changes").fire();
                assertEquals("Resolve active requests and deals before changing the listing type.", error(pane));

                choose(pane, "Listing type *", "Sell");
                assertTrue(FxTest.button(pane, "Remove").isVisible());
                FxTest.button(pane, "Remove").fire();
                FxTest.button(pane, "Remove").fire();
                assertTrue(FxTest.hasLabel(pane, "No photos selected."));

                FxTest.field(pane, "Item name *").setText("Edited lamp");
                saveButton(pane, "Save Changes").fire();
            });
        });
        Listing edited = result.get().orElseThrow();
        assertSame(existing, edited);
        assertEquals("Edited lamp", edited.getTitle());
        assertTrue(edited.getPhotoPaths().isEmpty());
        assertEquals(ListingStatus.LIVE, edited.getStatus());
    }

    @Test
    void editModeCoversLendGiveawayAndMissingSellerBranches() throws Exception {
        DummyDataService data = DummyDataService.getInstance();

        Listing lend = new Listing();
        lend.setId("FORM-LEND-1");
        lend.setSellerUsername("ghost-seller");
        lend.setSellerFullName("Ghost Seller");
        lend.setTitle("Borrowable drill");
        lend.setType(ListingType.LEND);
        lend.setCategory("Electronics");
        lend.setCondition("Used");
        lend.setDescription("Works well.");
        lend.setMeetupLocation("Library");
        lend.setLendingDays(4);
        lend.setStatus(ListingStatus.LIVE);
        lend.setPostedAt(LocalDateTime.now());
        data.addListing(lend);

        AtomicReference<Optional<Listing>> lendResult = new AtomicReference<>();
        FxTest.run(() -> {
            AdminListingFormDialog dialog = new AdminListingFormDialog(null, lend, null);
            FxTest.withDialog(() -> lendResult.set(dialog.showAndWait()), pane -> {
                assertEquals("Borrowable drill", FxTest.field(pane, "Item name *").getText());
                saveButton(pane, "Save Changes").fire();
            });
        });
        assertNotNull(lendResult.get().orElseThrow());

        Listing giveaway = new Listing();
        giveaway.setId("FORM-GIVEAWAY-1");
        giveaway.setSellerUsername("ghost-seller");
        giveaway.setSellerFullName("Ghost Seller");
        giveaway.setTitle("Free box");
        giveaway.setType(ListingType.GIVEAWAY);
        giveaway.setCategory("Others");
        giveaway.setCondition("Used");
        giveaway.setDescription("Take it away.");
        giveaway.setMeetupLocation("Library");
        giveaway.setStatus(ListingStatus.LIVE);
        giveaway.setPostedAt(LocalDateTime.now());
        data.addListing(giveaway);

        AtomicReference<Optional<Listing>> giveawayResult = new AtomicReference<>();
        FxTest.run(() -> {
            AdminListingFormDialog dialog = new AdminListingFormDialog(null, giveaway, null);
            FxTest.withDialog(() -> giveawayResult.set(dialog.showAndWait()), pane -> {
                choose(pane, "Listing type *", "Give Away");
                saveButton(pane, "Save Changes").fire();
            });
        });
        assertNotNull(giveawayResult.get().orElseThrow());

        Listing hiddenPrice = new Listing();
        hiddenPrice.setId("FORM-HIDDEN-1");
        hiddenPrice.setSellerUsername("ghost-seller");
        hiddenPrice.setSellerFullName("Ghost Seller");
        hiddenPrice.setTitle("Hidden price");
        hiddenPrice.setType(ListingType.SELL);
        hiddenPrice.setCategory("Others");
        hiddenPrice.setCondition("Used");
        hiddenPrice.setDescription("No price yet.");
        hiddenPrice.setMeetupLocation("Library");
        hiddenPrice.setStatus(ListingStatus.LIVE);
        hiddenPrice.setPostedAt(LocalDateTime.now());
        data.addListing(hiddenPrice);

        AtomicReference<Optional<Listing>> hiddenResult = new AtomicReference<>();
        FxTest.run(() -> {
            AdminListingFormDialog dialog = new AdminListingFormDialog(null, hiddenPrice, null);
            FxTest.withDialog(() -> hiddenResult.set(dialog.showAndWait()), pane -> {
                FxTest.field(pane, "Price in HK$ (required for Sell)").setText("12");
                saveButton(pane, "Save Changes").fire();
            });
        });
        assertEquals(12.0, hiddenResult.get().orElseThrow().getPrice());
    }
}
