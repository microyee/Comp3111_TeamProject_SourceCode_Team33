package com.hkust.marketplace;

import com.hkust.marketplace.models.Listing;
import com.hkust.marketplace.models.ListingType;
import com.hkust.marketplace.models.ListingStatus;
import com.hkust.marketplace.models.UserRole;
import com.hkust.marketplace.utils.MarketplaceAccounts;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SceneManager;
import com.hkust.marketplace.utils.SessionManager;
import com.hkust.marketplace.utils.SellerMarketplace;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.image.ImageView;
import java.nio.file.Path;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.ComboBox;
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
    void sellerBuyerAndAdminCompleteSaleThroughJavaFx() throws Exception {
        onFx(() -> {
            var data = DummyDataService.getInstance();
            var accounts = new MarketplaceAccounts(data);
            accounts.register(UserRole.SELLER, "market-test-seller", "Camera Seller", "Password1", "Password1", null, "", "Library");
            accounts.register(UserRole.BUYER, "market-test-buyer", "Camera Buyer", "Password1", "Password1", null, null, null);
            Stage stage = new Stage();
            SceneManager.init(stage);

            SceneManager.showSellerRegister();
            SceneManager.showSellerLogin();
            signIn(stage, "market-test-seller", "Password1", "Login");
            navigate(stage, "btnCreate", "Create Listing");
            Parent form = stage.getScene().getRoot();
            button(stage, "Submit for review").fire();
            assertTrue(hasLabel(form, "Choose a listing type."));
            setChoice(form, "Select listing type", ListingType.SELL);
            field(form, "Item name").setText("Integration film camera");
            setChoice(form, "Select category", "Electronics");
            setChoice(form, "Select condition", "Used");
            ((TextArea) form.lookup(".text-area")).setText("Tested with a roll of film");
            field(form, "Price in HKD").setText("99.50");
            button(stage, "Submit for review").fire();
            Listing listing = data.getListingsBySeller("market-test-seller").get(0);
            assertEquals(ListingStatus.PENDING_REVIEW, listing.getStatus());
            button(stage, "Logout").fire();

            SessionManager.login(data.findUser("admin"));
            SceneManager.showAdminDashboard();
            Parent reviewCard = cardWithLabel(stage, "Integration film camera");
            runDialogs(button(reviewCard, "Approve"), dialog -> button(dialog, "OK").fire(),
                    dialog -> button(dialog, "OK").fire());
            assertEquals(ListingStatus.LIVE, listing.getStatus());
            SessionManager.logout();

            SceneManager.showBuyerLogin();
            signIn(stage, "market-test-buyer", "Password1", "Login");
            field(stage.getScene().getRoot(), "Search listings").setText("Integration film camera");
            button(stage, "View listing").fire();
            runDialogs(button(stage, "Make offer / bid"), dialog -> {
                field(dialog, "Amount in HKD").setText("0");
                button(dialog, "Send").fire();
                assertTrue(hasLabel(dialog, "Enter a positive HKD amount with up to two decimal places."));
                field(dialog, "Amount in HKD").setText("80");
                button(dialog, "Send").fire();
            });
            assertEquals("My Deals", title(stage));
            button(stage, "Logout").fire();

            SceneManager.showSellerLogin();
            signIn(stage, "market-test-seller", "Password1", "Login");
            navigate(stage, "btnRequests", "Incoming Requests");
            runDialogs(button(stage, "Counter"), dialog -> {
                field(dialog, "Amount in HKD").setText("90");
                button(dialog, "Send").fire();
            });
            navigate(stage, "btnChat", "Messages");
            navigate(stage, "btnStats", "Seller Stats");
            navigate(stage, "btnReviews", "Reviews & Feedback");
            navigate(stage, "btnNotifications", "Notifications");
            navigate(stage, "btnProfile", "My Profile");
            button(stage, "Logout").fire();

            SceneManager.showBuyerLogin();
            signIn(stage, "market-test-buyer", "Password1", "Login");
            navigate(stage, "btnDeals", "My Deals");
            runDialogs(button(stage, "Accept counteroffer"), dialog -> button(dialog, "OK").fire());
            assertEquals(ListingStatus.RESERVED, listing.getStatus());
            runDialogs(button(stage, "Complete deal"), confirm -> button(confirm, "OK").fire(), review -> {
                setChoice(review, "Stars", 5);
                ((TextArea) review.lookup(".text-area")).setText("Camera is in great condition.");
                button(review, "Publish review").fire();
            });
            assertEquals(ListingStatus.SOLD, listing.getStatus());
            assertEquals("Deal History", title(stage));
            assertEquals(5.0, data.getPublicAverageRating("market-test-seller"));
            button(stage, "Logout").fire();

            SceneManager.showSellerLogin();
            signIn(stage, "market-test-seller", "Password1", "Login");
            navigate(stage, "btnReviews", "Reviews & Feedback");
            runDialogs(button(stage, "Reply"), dialog -> {
                ((TextArea) dialog.lookup(".text-area")).setText("Enjoy the camera!");
                button(dialog, "Send reply").fire();
            });
            runDialogs(button(stage, "Flag review"), dialog -> {
                ((TextArea) dialog.lookup(".text-area")).setText("Please check the context.");
                button(dialog, "Send report").fire();
            });
            assertEquals("Enjoy the camera!", data.getRatingsForSeller("market-test-seller").get(0).getSellerReply());
            SessionManager.login(data.findUser("admin"));
            SceneManager.showAdminDashboard();
            navigate(stage, "btnReports", "Moderation Reports");
            Parent report = cardWithLabel(stage, "Integration film camera · Open");
            button(report, "View rating").fire();
            assertTrue(hasLabel(stage.getScene().getRoot(), "Camera is in great condition."));
            runDialogs(button(stage, "Hide"), dialog -> button(dialog, "OK").fire());
            assertEquals(0, data.getPublicRatingCount("market-test-seller"));
            navigate(stage, "btnReports", "Moderation Reports");
            Parent openReport = cardWithLabel(stage, "Integration film camera · Open");
            runDialogs(button(openReport, "Resolve"), dialog -> {
                button(dialog, "Resolve").fire();
                assertTrue(hasLabel(dialog, "Resolution is required."));
                ((TextArea) dialog.lookup(".text-area")).setText("Reviewed and hidden.");
                button(dialog, "Resolve").fire();
            });
            assertTrue(data.catalog().reports(SessionManager.getCurrentUser()).stream()
                    .anyMatch(item -> !item.isOpen() && "Reviewed and hidden.".equals(item.resolution())));
            SessionManager.logout();
            stage.close();
        });
    }

    @Test
    void sellerCanCorrectRejectedListingAndRemoveTheResubmission() throws Exception {
        onFx(() -> {
            var data = DummyDataService.getInstance();
            var accounts = new MarketplaceAccounts(data);
            var seller = accounts.register(UserRole.SELLER, "test-loan-seller", "Loan Seller", "Password1", "Password1",
                    null, "", "Library");
            var marketplace = new SellerMarketplace(data);
            Listing item = marketplace.save(seller, null, new SellerMarketplace.ListingDetails(
                    ListingType.GIVEAWAY, "Test desk lamp", "Furniture", "Used", "Works well", "Library", null, null, List.of()));
            Stage stage = new Stage();
            SceneManager.init(stage);
            SessionManager.login(data.findUser("admin"));
            SceneManager.showAdminDashboard();
            Parent review = cardWithLabel(stage, item.getTitle());
            runDialogs(button(review, "Reject"), dialog -> {
                Button submit = button(dialog, "Submit");
                assertTrue(submit.isDisabled());
                ((TextArea) dialog.lookup(".text-area")).setText("Add a clearer description.");
                submit.fire();
            }, dialog -> button(dialog, "OK").fire());
            assertEquals(ListingStatus.REJECTED, item.getStatus());
            SessionManager.logout();

            SceneManager.showSellerLogin();
            signIn(stage, seller.getUsername(), "Password1", "Login");
            Parent row = cardWithLabel(stage, item.getTitle());
            assertTrue(hasLabel(row, "Review reason: Add a clearer description."));
            button(row, "Edit").fire();
            assertEquals("Edit Listing", title(stage));
            Parent edit = stage.getScene().getRoot();
            setChoice(edit, "Select listing type", ListingType.LEND);
            field(edit, "Maximum duration in days").setText("one");
            button(stage, "Save and resubmit").fire();
            assertTrue(hasLabel(edit, "Enter the loan duration in whole days."));
            field(edit, "Maximum duration in days").setText("1");
            runDialogs(button(stage, "Save and resubmit"), dialog -> button(dialog, "OK").fire());
            assertEquals(ListingStatus.PENDING_REVIEW, item.getStatus());
            assertEquals(ListingType.LEND, item.getType());
            assertEquals(1, item.getLendingDays());
            assertNull(item.getRejectionReason());

            Parent updated = cardWithLabel(stage, item.getTitle());
            runDialogs(button(updated, "Delete"), dialog -> button(dialog, "OK").fire());
            assertEquals(ListingStatus.REMOVED, item.getStatus());
            assertFalse(hasLabel(stage.getScene().getRoot(), item.getTitle()));
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

    @SuppressWarnings("unchecked")
    private static <T> void setChoice(Parent root, String name, T value) {
        root.applyCss();
        Node combo = root.lookupAll(".combo-box").stream().filter(node -> name.equals(node.getAccessibleText()))
                .findFirst().orElseThrow(() -> new AssertionError("No dropdown named " + name));
        ((ComboBox<T>) combo).setValue(value);
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
