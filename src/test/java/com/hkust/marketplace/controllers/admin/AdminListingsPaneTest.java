package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Buyer;
import com.hkust.marketplace.models.Listing;
import com.hkust.marketplace.models.ListingStatus;
import com.hkust.marketplace.models.ListingType;
import com.hkust.marketplace.models.Seller;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.testsupport.FxTest;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.MarketplaceActivity;
import com.hkust.marketplace.utils.SessionManager;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminListingsPaneTest {

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

    private static TableView<?> table(Parent root) {
        return (TableView<?>) root.lookup(".table-view");
    }

    private static Listing listing(DummyDataService data, String title, ListingType type,
                                   ListingStatus status, String seller, List<String> photos,
                                   LocalDateTime postedAt) {
        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setDescription("Listing for coverage " + title);
        listing.setType(type);
        listing.setCategory("Electronics");
        listing.setCondition("Used");
        listing.setMeetupLocation("Library");
        listing.setPrice(type == ListingType.SELL ? 10.0 : null);
        listing.setLendingDays(type == ListingType.LEND ? 3 : null);
        listing.setSellerUsername(seller);
        listing.setSellerFullName(data.findUser(seller) == null ? seller : data.findUser(seller).getFullName());
        listing.setStatus(status);
        listing.setPostedAt(postedAt);
        listing.setPhotoPaths(photos);
        data.addListing(listing);
        return listing;
    }

    private static void invoke(Object target, String method, Class<?>[] types, Object... args) {
        try {
            Method declared = target.getClass().getDeclaredMethod(method, types);
            declared.setAccessible(true);
            declared.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw new AssertionError(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void filtersHighlightsAndCellRendering() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        SessionManager.login(data.findUser("admin"));
        User seller = new Seller("listings-filter-seller", "Listings Filter Seller", "Password1");
        data.registerUser(seller);
        Listing sell = listing(data, "Listings filter SELL", ListingType.SELL, ListingStatus.LIVE,
                seller.getUsername(), List.of("/images/hkust_logo.png"), LocalDateTime.now().minusDays(1));
        Listing lend = listing(data, "Listings filter LEND", ListingType.LEND, ListingStatus.PENDING_REVIEW,
                seller.getUsername(), List.of(), LocalDateTime.now().minusDays(2));
        Listing giveaway = listing(data, "Listings filter GIVEAWAY", ListingType.GIVEAWAY, ListingStatus.REMOVED,
                seller.getUsername(), List.of(), null);

        FxTest.run(() -> {
            AdminListingsPane pane = new AdminListingsPane(new AdminDashboardController(), sell.getId());
            Stage stage = FxTest.stage(pane);
            TableView<?> table = table(pane);
            TextField search = FxTest.field(pane, "Search title, seller, category or listing ID...");
            assertEquals(sell.getId(), search.getText());
            assertTrue(table.getItems().size() >= 1);

            search.setText("Listings filter LEND");
            assertEquals(1, table.getItems().size());
            search.setText(seller.getUsername());
            assertEquals(3, table.getItems().size());
            search.setText("Listings Filter Seller");
            assertEquals(3, table.getItems().size());
            search.setText("Electronics");
            assertTrue(table.getItems().size() >= 3);
            search.setText("coverage");
            assertTrue(table.getItems().size() >= 3);
            search.setText("no-such-listing");
            assertEquals(0, table.getItems().size());
            search.clear();

            ComboBox<String> status = comboWith(pane, "All Statuses");
            for (String value : List.of("Live", "Pending Review", "Rejected", "Reserved",
                    "Sold", "Lent", "Given", "Removed")) {
                status.setValue(value);
                assertTrue(table.getItems().stream()
                        .allMatch(item -> value.equals(AdminUi.statusText(((Listing) item).getStatus()))));
            }
            status.setValue("All Statuses");

            ComboBox<String> type = comboWith(pane, "All Types");
            for (String value : List.of("For Sale", "For Lending", "Giveaway")) {
                type.setValue(value);
                assertTrue(table.getItems().stream()
                        .allMatch(item -> value.equals(((Listing) item).getTypeDisplay())));
            }
            type.setValue("All Types");

            table.refresh();
            pane.applyCss();
            pane.layout();
            stage.hide();
        });
        SessionManager.logout();

        SessionManager.login(data.findUser("admin"));
        FxTest.run(() -> {
            AdminListingsPane pane = new AdminListingsPane(new AdminDashboardController(), "missing-id");
            Stage stage = FxTest.stage(pane);
            assertEquals("missing-id", FxTest.field(pane, "Search title, seller, category or listing ID...").getText());
            stage.hide();
        });
        SessionManager.logout();
    }

    @Test
    void addEditTakeDownAndStaleListingBranches() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        SessionManager.login(data.findUser("admin"));
        User seller = new Seller("listings-action-seller", "Listings Action Seller", "Password1");
        User buyer = new Buyer("listings-action-buyer", "Listings Action Buyer", "Password1");
        data.registerUser(seller);
        data.registerUser(buyer);
        Listing live = listing(data, "Listings action LIVE", ListingType.SELL, ListingStatus.LIVE,
                seller.getUsername(), List.of(), LocalDateTime.now());
        data.activity().submit(buyer, live.getId(), new MarketplaceActivity.Terms(5.0, null, null));
        Listing cancelTarget = listing(data, "Listings action CANCEL", ListingType.SELL, ListingStatus.LIVE,
                seller.getUsername(), List.of(), LocalDateTime.now());
        Listing stale = listing(data, "Listings action STALE", ListingType.SELL, ListingStatus.LIVE,
                seller.getUsername(), List.of(), LocalDateTime.now());
        Listing removed = listing(data, "Listings action REMOVED", ListingType.SELL, ListingStatus.REMOVED,
                seller.getUsername(), List.of(), LocalDateTime.now());

        var paneRef = new java.util.concurrent.atomic.AtomicReference<AdminListingsPane>();
        FxTest.run(() -> {
            AdminListingsPane pane = new AdminListingsPane(new AdminDashboardController(), null);
            Stage stage = FxTest.stage(pane);
            paneRef.set(pane);
        });

        FxTest.run(() -> FxTest.withDialog(
                () -> invoke(paneRef.get(), "addListing", new Class<?>[0]), pane -> {
                    ComboBox<String> type = FxTest.combo(pane, "Listing type *", String.class);
                    type.setValue("Sell");
                    FxTest.field(pane, "Item name *").setText("Listings action CREATED");
                    FxTest.combo(pane, "Category *", String.class).setValue("Electronics");
                    FxTest.combo(pane, "Condition *", String.class).setValue("Used");
                    FxTest.combo(pane, "Meet-up location *", String.class).setValue("Library");
                    ((TextArea) pane.lookup(".text-area")).setText("Created by an admin.");
                    ComboBox<?> sellerCombo = FxTest.combo(pane, "Choose a seller *");
                    sellerCombo.getSelectionModel().selectFirst();
                    FxTest.field(pane, "Price in HK$ (required for Sell)").setText("42");
                    FxTest.button(pane, "Create Listing").fire();
                }, dialog -> FxTest.button(dialog, "OK").fire()));
        assertTrue(data.getAllListings().stream()
                .anyMatch(item -> "Listings action CREATED".equals(item.getTitle())));

        FxTest.run(() -> FxTest.withDialog(
                () -> invoke(paneRef.get(), "editListing", new Class<?>[]{Listing.class}, live),
                dialog -> {
                    assertEquals("Edit listing: Listings action LIVE", dialog.getHeaderText());
                    FxTest.button(dialog, "Save Changes").fire();
                }, dialog -> FxTest.button(dialog, "OK").fire()));

        FxTest.run(() -> FxTest.withDialog(
                () -> invoke(paneRef.get(), "editListing", new Class<?>[]{Listing.class}, removed),
                dialog -> FxTest.button(dialog, "Cancel").fire()));

        data.removeListing(removed.getId());
        FxTest.run(() -> FxTest.withDialog(
                () -> invoke(paneRef.get(), "editListing", new Class<?>[]{Listing.class}, removed),
                dialog -> {
                    assertTrue(FxTest.hasLabel(dialog, "Listing no longer available"));
                    FxTest.button(dialog, "OK").fire();
                }));

        FxTest.run(() -> FxTest.withDialog(
                () -> invoke(paneRef.get(), "takeDownListing", new Class<?>[]{Listing.class}, cancelTarget),
                dialog -> FxTest.button(dialog, "Cancel").fire()));
        assertEquals(ListingStatus.LIVE, cancelTarget.getStatus());

        FxTest.run(() -> FxTest.withDialog(
                () -> invoke(paneRef.get(), "takeDownListing", new Class<?>[]{Listing.class}, live),
                dialog -> {
                    TextArea area = (TextArea) dialog.lookup(".text-area");
                    assertTrue(FxTest.button(dialog, "Submit").isDisabled());
                    area.setText("  Policy violation  ");
                    FxTest.button(dialog, "Submit").fire();
                }, dialog -> {
                    assertTrue(FxTest.hasLabel(dialog, "Confirm take down"));
                    FxTest.button(dialog, "Take Down").fire();
                }, dialog -> FxTest.button(dialog, "OK").fire()));
        assertEquals(ListingStatus.REMOVED, live.getStatus());
        assertEquals("Policy violation", live.getRemovalReason());
        assertTrue(data.getActiveRequestsForListing(live.getId()).isEmpty());

        FxTest.run(() -> FxTest.withDialog(
                () -> invoke(paneRef.get(), "takeDownListing", new Class<?>[]{Listing.class}, stale),
                dialog -> {
                    stale.setStatus(ListingStatus.REMOVED);
                    data.updateListing(stale);
                    ((TextArea) dialog.lookup(".text-area")).setText("Changed while confirming");
                    FxTest.button(dialog, "Submit").fire();
                }, dialog -> FxTest.button(dialog, "Take Down").fire(),
                dialog -> FxTest.button(dialog, "OK").fire()));

        Listing missing = listing(data, "Listings action MISSING", ListingType.SELL, ListingStatus.LIVE,
                seller.getUsername(), List.of(), LocalDateTime.now());
        data.removeListing(missing.getId());
        FxTest.run(() -> FxTest.withDialog(
                () -> invoke(paneRef.get(), "takeDownListing", new Class<?>[]{Listing.class}, missing),
                dialog -> {
                    assertTrue(FxTest.hasLabel(dialog, "Listing no longer live"));
                    FxTest.button(dialog, "OK").fire();
                }));

        FxTest.run(() -> FxTest.withDialogs(FxTest.button(paneRef.get(), "+ Add Listing"),
                dialog -> FxTest.button(dialog, "Cancel").fire()));
        FxTest.run(() -> paneRef.get().getScene().getWindow().hide());
        SessionManager.logout();
    }
}
