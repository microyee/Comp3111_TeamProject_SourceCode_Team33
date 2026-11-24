package com.hkust.marketplace;

import com.hkust.marketplace.models.UserRole;
import com.hkust.marketplace.utils.MarketplaceAccounts;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SceneManager;
import com.hkust.marketplace.utils.SessionManager;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.image.ImageView;
import java.nio.file.Path;
import javafx.scene.control.TextField;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
        String photo = Path.of(MarketplaceNavigationTest.class.getResource("/images/hkust_logo.png").toURI()).toString();
        onFx(() -> {
            Stage stage = new Stage();
            SceneManager.init(stage);
            SceneManager.showWelcome();
            SceneManager.showBuyerRegister();
            Button register = button(stage, "Create Account");
            Parent registration = stage.getScene().getRoot();
            register.fire();
            assertTrue(hasLabel(registration, "Use 3–32 characters for your username, starting with a letter or number."));
            ((TextField) registration.lookup("#usernameField")).setText("buyer-ui-test");
            ((TextField) registration.lookup("#fullNameField")).setText("Browser Test");
            ((TextField) registration.lookup("#passwordField")).setText("Password1");
            ((TextField) registration.lookup("#confirmPasswordField")).setText("Password1");
            register.fire();
            assertNotNull(DummyDataService.getInstance().findUser("buyer-ui-test"));
            SceneManager.showBuyerLogin();
            signIn(stage, "alice", "wrong", "Login");
            assertTrue(hasLabel(stage.getScene().getRoot(), "Incorrect username or password."));
            var listing = DummyDataService.getInstance().getLiveListings().get(0);
            listing.setPhotoPaths(List.of(photo));
            signIn(stage, "alice", "password1", "Login");
            assertEquals("alice", SessionManager.getCurrentUser().getUsername());
            assertEquals("Browse Marketplace", title(stage));
            for (String[] page : List.of(new String[]{"btnDeals", "My Deals"},
                    new String[]{"btnHistory", "Deal History"}, new String[]{"btnFavourites", "Favourites"},
                    new String[]{"btnFollowing", "Following"}, new String[]{"btnChat", "Messages"},
                    new String[]{"btnNotifications", "Notifications"}, new String[]{"btnProfile", "My Profile"},
                    new String[]{"btnBrowse", "Browse Marketplace"})) navigate(stage, page[0], page[1]);
            Parent browse = stage.getScene().getRoot();
            field(browse, "Min HKD").setText("12x");
            assertTrue(hasLabel(browse, "Enter a valid number."));
            field(browse, "Min HKD").clear();
            prompt(browse, "Search listings").setText(listing.getTitle());
            button(stage, "View listing").fire();
            assertEquals("Listing Details", title(stage));
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
            assertTrue(stage.getScene().getRoot().lookupAll(".listing-photo").stream()
                    .filter(ImageView.class::isInstance).map(ImageView.class::cast)
                    .anyMatch(view -> view.getImage().getRequestedWidth() == 220 && !view.getImage().isError()));
            button(stage, "Save favourite").fire();
            button(stage, "Seller profile").fire();
            assertEquals("Seller Profile", title(stage));
            button(stage, "Follow seller").fire();
            button(stage, "Back").fire();
            assertEquals("Listing Details", title(stage));
            button(stage, "Back").fire();
            assertEquals("Browse Marketplace", title(stage));
            assertEquals(listing.getTitle(), prompt(stage.getScene().getRoot(), "Search listings").getText());
            listing.setPhotoPaths(List.of());
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

    @Test
    void adminCanManageAccountsAndProfileThroughJavaFx() throws Exception {
        onFx(() -> {
            Stage stage = new Stage();
            SceneManager.init(stage);
            var data = DummyDataService.getInstance();
            var admin = data.findUser("admin");
            String originalName = admin.getFullName();
            String originalHash = admin.getPasswordHash();
            try {
                SceneManager.showAdminLogin();
                signIn(stage, "alice", "password1", "Login as Admin");
                assertTrue(hasLabel(stage.getScene().getRoot(), "This account is not an Administrator account. "
                        + "Please use the Buyer or Seller portal."));
                signIn(stage, "admin", "wrong", "Login as Admin");
                assertTrue(hasLabel(stage.getScene().getRoot(), "Incorrect password. Please try again."));
                signIn(stage, "admin", "admin123", "Login as Admin");

                navigate(stage, "btnUsers", "Manage All Users");
                runDialogs(button(stage, "+ Add User"), dialog -> {
                    button(dialog, "Create User").fire();
                    assertTrue(hasLabel(dialog, "Full Name cannot be empty."));
                    prompt(dialog, "e.g. Chan Tai Man").setText("Admin Created Buyer");
                    prompt(dialog, "e.g. chan123").setText("alice");
                    prompt(dialog, "Initial password").setText("Password1");
                    button(dialog, "Create User").fire();
                    assertTrue(hasLabel(dialog, "Username already exists. Choose another."));
                    prompt(dialog, "e.g. chan123").setText("admin-test-buyer");
                    button(dialog, "Create User").fire();
                }, dialog -> button(dialog, "OK").fire());
                var user = data.findUser("admin-test-buyer");
                assertNotNull(user);
                assertEquals(UserRole.BUYER, user.getRole());

                Parent row = cardWithLabel(stage, "@admin-test-buyer");
                runDialogs(button(row, "Edit"), dialog -> {
                    ((TextField) dialog.lookup(".text-field")).setText("Updated Buyer");
                    button(dialog, "Save Changes").fire();
                }, dialog -> button(dialog, "OK").fire());
                assertEquals("Updated Buyer", user.getFullName());
                row = cardWithLabel(stage, "@admin-test-buyer");
                runDialogs(button(row, "Deactivate"), dialog -> button(dialog, "Deactivate").fire(),
                        dialog -> button(dialog, "OK").fire());
                assertFalse(user.isActive());
                row = cardWithLabel(stage, "@admin-test-buyer");
                runDialogs(button(row, "Reactivate"), dialog -> button(dialog, "Reactivate").fire(),
                        dialog -> button(dialog, "OK").fire());
                assertTrue(user.isActive());
                row = cardWithLabel(stage, "@admin-test-buyer");
                runDialogs(button(row, "Delete"), dialog -> button(dialog, "Delete account").fire(),
                        dialog -> button(dialog, "OK").fire());
                assertTrue(user.isDeleted());
                runDialogs(button(stage, "Audit Log"), dialog -> button(dialog, "Close").fire());

                navigate(stage, "btnProfile", "My Profile");
                Parent profile = stage.getScene().getRoot();
                prompt(profile, "Full name *").setText("");
                button(stage, "Save Changes").fire();
                assertTrue(hasLabel(profile, "Full Name cannot be empty."));
                prompt(profile, "Full name *").setText("Updated Admin");
                prompt(profile, "Current password").setText("wrong");
                prompt(profile, "New password").setText("Password2");
                prompt(profile, "Confirm new password").setText("Password2");
                button(stage, "Save Changes").fire();
                assertTrue(hasLabel(profile, "Current password is incorrect."));
                prompt(profile, "Current password").setText("admin123");
                prompt(profile, "Confirm new password").setText("Password3");
                button(stage, "Save Changes").fire();
                assertTrue(hasLabel(profile, "New password and confirmation do not match."));
                prompt(profile, "Confirm new password").setText("Password2");
                button(stage, "Save Changes").fire();
                assertEquals("Updated Admin", admin.getFullName());
                assertTrue(hasLabel(profile, "Profile updated. Your password was changed and hashed."));
                assertSame(admin, new MarketplaceAccounts(data).authenticate(UserRole.ADMIN, "admin", "Password2"));

                navigate(stage, "btnNotifications", "Admin Notifications");
                button(stage, "Mark all as read").fire();
                assertEquals(0, data.getUnreadCount("admin"));
            } finally {
                admin.setFullName(originalName);
                admin.setPasswordHash(originalHash);
                SessionManager.logout();
                stage.close();
            }
        });
    }

    private static TextField prompt(Parent root, String prompt) {
        root.applyCss();
        return root.lookupAll(".text-input").stream().filter(TextField.class::isInstance).map(TextField.class::cast)
                .filter(field -> prompt.equals(field.getPromptText())).findFirst()
                .orElseThrow(() -> new AssertionError("No prompt named " + prompt));
    }

    @SafeVarargs
    private static void runDialogs(Button trigger, Consumer<DialogPane>... steps) {
        var error = new AtomicReference<Throwable>();
        scheduleDialog(steps, 0, error);
        trigger.fire();
        if (error.get() != null) throw new AssertionError("Dialog interaction failed", error.get());
    }

    private static void scheduleDialog(Consumer<DialogPane>[] steps, int index, AtomicReference<Throwable> error) {
        Platform.runLater(() -> {
            if (error.get() != null) return;
            try {
                DialogPane dialog = visibleDialog();
                if (index + 1 < steps.length) scheduleDialog(steps, index + 1, error);
                steps[index].accept(dialog);
            } catch (Throwable e) {
                error.set(e);
                for (Window window : List.copyOf(Window.getWindows())) {
                    if (window.isShowing() && window.getScene().getRoot() instanceof DialogPane) window.hide();
                }
            }
        });
    }

    private static DialogPane visibleDialog() {
        return Window.getWindows().stream().filter(Window::isShowing)
                .map(window -> window.getScene().getRoot()).filter(DialogPane.class::isInstance)
                .map(DialogPane.class::cast).reduce((first, last) -> last)
                .orElseThrow(() -> new AssertionError("No visible dialog"));
    }

    private static Parent cardWithLabel(Stage stage, String text) {
        Parent root = stage.getScene().getRoot();
        root.applyCss();
        root.layout();
        return root.lookupAll(".card").stream().filter(Parent.class::isInstance).map(Parent.class::cast)
                .filter(pane -> hasLabel(pane, text)).findFirst()
                .orElseThrow(() -> new AssertionError("No card for " + text));
    }

    private static boolean hasLabel(Parent root, String text) {
        root.applyCss();
        return root.lookupAll(".label").stream().filter(Labeled.class::isInstance).map(Labeled.class::cast)
                .anyMatch(label -> text.equals(label.getText()));
    }

    private static TextField field(Parent root, String name) {
        root.applyCss();
        return root.lookupAll(".text-field").stream().filter(TextField.class::isInstance).map(TextField.class::cast)
                .filter(field -> name.equals(field.getAccessibleText())).findFirst()
                .orElseThrow(() -> new AssertionError("No field named " + name));
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
        return button(stage.getScene().getRoot(), text);
    }

    private static Button button(Parent root, String text) {
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
        task.get(45, TimeUnit.SECONDS);
    }
}
