package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.User;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SceneManager;
import com.hkust.marketplace.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Task 3 – Admin Portal shell.
 *
 * <p>The dashboard owns navigation, the unread-notification badge and session guarding.
 * Each section is implemented in its own pane class so the business screens stay
 * readable and independently testable.</p>
 */
public class AdminDashboardController {

    @FXML private StackPane contentPane;
    @FXML private Label pageTitle;
    @FXML private Label welcomeLabel;
    @FXML private Label unreadLabel;
    @FXML private Button btnReview;
    @FXML private Button btnAllListings;
    @FXML private Button btnUsers;
    @FXML private Button btnDeals;
    @FXML private Button btnRatings;
    @FXML private Button btnNotifications;
    @FXML private Button btnProfile;

    private Button currentActiveBtn;

    @FXML
    public void initialize() {
        // Role-based access control at the screen boundary: a buyer/seller session (or
        // a direct navigation without a session) cannot open the admin dashboard.
        if (!SessionManager.isAdmin() || !SessionManager.isSessionValid()) {
            SessionManager.logout();
            Platform.runLater(SceneManager::showAdminLogin);
            return;
        }

        refreshWelcome();
        currentActiveBtn = btnReview;
        refreshUnreadBadge();
        showReview();
    }

    private void setActive(Button button, String title) {
        if (currentActiveBtn != null) {
            currentActiveBtn.getStyleClass().remove("sidebar-btn-active");
        }
        if (button != null && !button.getStyleClass().contains("sidebar-btn-active")) {
            button.getStyleClass().add("sidebar-btn-active");
        }
        currentActiveBtn = button;
        pageTitle.setText(title);
    }

    private void setPane(Node pane) {
        contentPane.getChildren().setAll(pane);
    }

    public void refreshWelcome() {
        User user = SessionManager.getCurrentUser();
        if (welcomeLabel != null) {
            welcomeLabel.setText(user == null ? "" : "Admin: " + user.getFullName());
        }
    }

    /** Updates the header badge after notifications are read or a new one arrives. */
    public void refreshUnreadBadge() {
        if (unreadLabel == null) return;
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        int unread = DummyDataService.getInstance().getUnreadCount(user.getUsername());
        unreadLabel.getStyleClass().setAll("badge", unread > 0 ? "badge-pending" : "badge-live");
        unreadLabel.setText(unread > 0 ? "🔔 " + unread + " unread" : "All notifications read");
        unreadLabel.setVisible(true);
        unreadLabel.setManaged(true);
    }

    /** Navigation target used by the notification board. */
    public void openNotificationTarget(String screen, String relatedId) {
        if (screen == null) {
            showNotifications();
            return;
        }
        switch (screen) {
            case "REVIEW" -> showReview();
            case "LISTINGS" -> showAllListings(relatedId);
            case "USERS" -> showUsers(relatedId);
            case "DEALS" -> showDeals();
            case "RATINGS" -> showRatings(relatedId);
            default -> showNotifications();
        }
    }

    // =====================================================================
    // Task 3.2 – Listing review and approval
    // =====================================================================

    @FXML
    private void showReview() {
        setActive(btnReview, "Listing Review & Approval");
        setPane(new AdminReviewPane(this));
    }

    // =====================================================================
    // Task 3.7 – Manage all listings
    // =====================================================================

    @FXML
    private void showAllListings() {
        showAllListings(null);
    }

    public void showAllListings(String highlightId) {
        setActive(btnAllListings, "Manage All Listings");
        setPane(new AdminListingsPane(this, highlightId));
    }

    // =====================================================================
    // Task 3.3 – Manage all users
    // =====================================================================

    @FXML
    private void showUsers() {
        showUsers(null);
    }

    public void showUsers(String initialSearch) {
        setActive(btnUsers, "Manage All Users");
        setPane(new AdminUsersPane(this, initialSearch));
    }

    // =====================================================================
    // Task 3.5 – All deals record
    // =====================================================================

    @FXML
    private void showDeals() {
        setActive(btnDeals, "All Deals Record");
        setPane(new AdminDealsPane(this));
    }

    // =====================================================================
    // Task 3.8 – Seller ratings overview
    // =====================================================================

    @FXML
    private void showRatings() {
        showRatings(null);
    }

    public void showRatings(String initialSearch) {
        setActive(btnRatings, "Seller Ratings Overview");
        setPane(new AdminRatingsPane(this, initialSearch));
    }

    // =====================================================================
    // Task 3.6 – Notification board
    // =====================================================================

    @FXML
    private void showNotifications() {
        setActive(btnNotifications, "Admin Notifications");
        setPane(new AdminNotificationsPane(this));
    }

    // =====================================================================
    // Task 3.4 – Manage own profile
    // =====================================================================

    @FXML
    private void showProfile() {
        setActive(btnProfile, "My Profile");
        setPane(new AdminProfilePane(this));
    }

    @FXML
    private void logout() {
        if (AdminDialogs.confirm("Log out", "Sign out of the Admin Portal?")) {
            SessionManager.logout();
            SceneManager.showWelcome();
        }
    }
}
