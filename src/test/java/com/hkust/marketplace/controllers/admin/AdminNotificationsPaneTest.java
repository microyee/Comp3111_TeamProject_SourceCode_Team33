package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Admin;
import com.hkust.marketplace.models.Notification;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.testsupport.FxTest;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SessionManager;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AdminNotificationsPaneTest {

    @BeforeAll
    static void startFx() {
        FxTest.ensureFx();
    }

    private static Parent rowWith(Parent root, String title) {
        FxTest.layout(root);
        return root.lookupAll(".notif-item").stream()
                .filter(Parent.class::isInstance)
                .map(Parent.class::cast)
                .filter(row -> FxTest.hasLabel(row, title))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No notification row for " + title));
    }

    @Test
    void opensEveryRelatedScreenAndHandlesMissingTargets() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        User admin = new Admin("notif-admin", "Notification Admin", "Password1");
        data.registerUser(admin);
        SessionManager.login(admin);

        var listing = data.getAllListings().iterator().next();
        var user = data.findUser("alice");
        var deal = data.getAllDeals().get(0);
        var rating = data.getAllRatings().isEmpty() ? null : data.getAllRatings().get(0);
        User deleted = new com.hkust.marketplace.models.Buyer("notif-deleted", "Deleted User", "Password1");
        data.registerUser(deleted);
        data.deleteUser(deleted.getUsername(), "admin");

        Notification listings = new Notification(admin.getUsername(), "N listings", "Message",
                "Submission", "LISTINGS", listing.getId());
        Notification users = new Notification(admin.getUsername(), "N users", "Message",
                "Account", "USERS", user.getUsername());
        Notification deals = new Notification(admin.getUsername(), "N deals", "Message",
                "Offer", "DEALS", deal.getId());
        Notification ratings = rating == null ? null : new Notification(admin.getUsername(),
                "N ratings", "Message", "Report", "RATINGS", rating.getId());
        Notification other = new Notification(admin.getUsername(), "N other", "Message",
                "System", "OTHER", "whatever");
        Notification missingListing = new Notification(admin.getUsername(), "N missing listing",
                "Message", "Submission", "LISTINGS", "missing-listing");
        Notification missingDeal = new Notification(admin.getUsername(), "N missing deal",
                "Message", "Offer", "DEALS", "missing-deal");
        Notification missingRating = new Notification(admin.getUsername(), "N missing rating",
                "Message", "Report", "RATINGS", "missing-rating");
        Notification deletedUserNotice = new Notification(admin.getUsername(), "N deleted user",
                "Message", "Account", "USERS", deleted.getUsername());
        Notification plain = new Notification(admin.getUsername(), "N plain", "Message", "System");
        for (Notification notification : java.util.Arrays.asList(listings, users, deals, ratings,
                other, missingListing, missingDeal, missingRating, deletedUserNotice, plain)) {
            if (notification != null) {
                data.addNotification(notification);
            }
        }
        plain.setRead(true);

        AtomicReference<String> openedScreen = new AtomicReference<>();
        AtomicReference<String> openedId = new AtomicReference<>();
        AdminDashboardController host = new AdminDashboardController() {
            @Override
            public void openNotificationTarget(String screen, String relatedId) {
                openedScreen.set(screen);
                openedId.set(relatedId);
            }
        };

        FxTest.run(() -> {
            AdminNotificationsPane pane = new AdminNotificationsPane(host);
            Stage stage = FxTest.stage(pane);
            assertTrue(pane.lookupAll(".notif-item").size() >= 10);

            FxTest.withDialogs(FxTest.button(rowWith(pane, "N listings"), "Open"), dialog -> {
                assertNotNull(dialog.lookupButton(dialog.getButtonTypes().get(1)));
                FxTest.button(dialog, "Open related screen").fire();
            });
            assertEquals("LISTINGS", openedScreen.get());
            assertEquals(listing.getId(), openedId.get());
            assertTrue(listings.isRead());

            FxTest.withDialogs(FxTest.button(rowWith(pane, "N users"), "Open"), dialog ->
                    FxTest.button(dialog, "Open related screen").fire());
            assertEquals("USERS", openedScreen.get());

            FxTest.withDialogs(FxTest.button(rowWith(pane, "N deals"), "Open"), dialog ->
                    FxTest.button(dialog, "Open related screen").fire());
            assertEquals("DEALS", openedScreen.get());

            if (ratings != null) {
                FxTest.withDialogs(FxTest.button(rowWith(pane, "N ratings"), "Open"), dialog ->
                        FxTest.button(dialog, "Open related screen").fire());
                assertEquals("RATINGS", openedScreen.get());
            }

            FxTest.withDialogs(FxTest.button(rowWith(pane, "N other"), "Open"), dialog ->
                    FxTest.button(dialog, "Open related screen").fire());
            assertEquals("OTHER", openedScreen.get());

            FxTest.withDialogs(FxTest.button(rowWith(pane, "N missing listing"), "Open"), dialog -> {
                assertTrue(FxTest.hasLabel(dialog, "The related listing or account is no longer available. "
                        + "The notification details above are retained."));
                FxTest.button(dialog, "Close").fire();
            });
            FxTest.withDialogs(FxTest.button(rowWith(pane, "N missing deal"), "Open"), dialog ->
                    FxTest.button(dialog, "Close").fire());
            FxTest.withDialogs(FxTest.button(rowWith(pane, "N missing rating"), "Open"), dialog ->
                    FxTest.button(dialog, "Close").fire());
            FxTest.withDialogs(FxTest.button(rowWith(pane, "N deleted user"), "Open"), dialog ->
                    FxTest.button(dialog, "Close").fire());

            FxTest.withDialogs(FxTest.button(rowWith(pane, "N plain"), "Details"), dialog ->
                    FxTest.button(dialog, "Close").fire());

            FxTest.button(pane, "Refresh").fire();
            FxTest.button(pane, "Mark all as read").fire();
            assertEquals(0, data.getUnreadCount(admin.getUsername()));
            stage.hide();
        });
        SessionManager.logout();
    }

    @Test
    void emptyNotificationBoardShowsTheEmptyState() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        User empty = new Admin("notif-empty-admin", "Empty Admin", "Password1");
        data.registerUser(empty);
        SessionManager.login(empty);

        FxTest.run(() -> {
            AdminNotificationsPane pane = new AdminNotificationsPane(new AdminDashboardController());
            Stage stage = FxTest.stage(pane);
            assertTrue(FxTest.hasLabel(pane, "No notifications"));
            assertTrue(FxTest.hasLabel(pane, "No notifications."));
            stage.hide();
        });
        SessionManager.logout();
    }
}
