package com.hkust.marketplace;

import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SceneManager;
import com.hkust.marketplace.utils.SessionManager;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MarketplaceNavigationTest {
    @BeforeAll
    static void startJavaFx() throws InterruptedException {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        assumeTrue(!os.contains("linux") || System.getenv("DISPLAY") != null,
                "JavaFX smoke tests require a display. On Linux, use xvfb-run.");
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            ready.countDown();
        });
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }

    @Test
    void buyerLoginAndNavigationLoadEveryScreen() throws Exception {
        onFx(() -> {
            Stage stage = new Stage();
            SceneManager.init(stage);
            SceneManager.showWelcome();
            SceneManager.showBuyerRegister();
            SceneManager.showBuyerLogin();
            signIn(stage, "alice", "password1", "Login");
            assertEquals("alice", SessionManager.getCurrentUser().getUsername());
            assertEquals("Browse Marketplace", title(stage));
            for (String[] page : List.of(new String[]{"btnDeals", "My Deals"},
                    new String[]{"btnHistory", "Deal History"}, new String[]{"btnFavourites", "Favourites"},
                    new String[]{"btnFollowing", "Following"}, new String[]{"btnChat", "Messages"},
                    new String[]{"btnNotifications", "Notifications"}, new String[]{"btnProfile", "My Profile"},
                    new String[]{"btnBrowse", "Browse Marketplace"})) navigate(stage, page[0], page[1]);
            button(stage, "View listing").fire();
            assertEquals("Listing Details", title(stage));
            button(stage, "Save favourite").fire();
            button(stage, "Seller profile").fire();
            assertEquals("Seller Profile", title(stage));
            button(stage, "Follow seller").fire();
            button(stage, "Back").fire();
            assertEquals("Listing Details", title(stage));
            button(stage, "Back").fire();
            assertEquals("Browse Marketplace", title(stage));
            SessionManager.logout();
            stage.close();
        });
    }

    @Test
    void adminDashboardLoadsReportsAndExistingScreens() throws Exception {
        onFx(() -> {
            Stage stage = new Stage();
            SceneManager.init(stage);
            SceneManager.showAdminLogin();
            var admin = DummyDataService.getInstance().findUser("admin");
            SessionManager.login(admin);
            SceneManager.showAdminDashboard();
            assertEquals("Listing Review & Approval", title(stage));
            for (String[] page : List.of(new String[]{"btnAllListings", "Manage All Listings"},
                    new String[]{"btnUsers", "Manage All Users"}, new String[]{"btnDeals", "All Deals Record"},
                    new String[]{"btnRatings", "Seller Ratings Overview"}, new String[]{"btnNotifications", "Admin Notifications"},
                    new String[]{"btnProfile", "My Profile"}, new String[]{"btnReports", "Moderation Reports"})) {
                navigate(stage, page[0], page[1]);
            }
            button(stage, "View listing").fire();
            assertEquals("Manage All Listings", title(stage));
            SessionManager.logout();
            stage.close();
        });
    }

    private static void signIn(Stage stage, String name, String password, String buttonText) {
        Parent root = stage.getScene().getRoot();
        ((TextField) root.lookup("#usernameField")).setText(name);
        ((TextField) root.lookup("#passwordField")).setText(password);
        button(stage, buttonText).fire();
    }

    private static void navigate(Stage stage, String id, String title) {
        ((Button) stage.getScene().getRoot().lookup("#" + id)).fire();
        assertEquals(title, title(stage));
    }

    private static Button button(Stage stage, String text) {
        Parent root = stage.getScene().getRoot();
        root.applyCss();
        root.layout();
        return root.lookupAll(".button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> text.equalsIgnoreCase(b.getText().strip())).findFirst()
                .orElseThrow(() -> new AssertionError("No button with text " + text));
    }

    private static String title(Stage stage) {
        return ((Labeled) stage.getScene().getRoot().lookup("#pageTitle")).getText();
    }

    private static void onFx(Runnable work) throws Exception {
        FutureTask<Void> task = new FutureTask<>(work, null);
        Platform.runLater(task);
        task.get(20, TimeUnit.SECONDS);
    }
}
