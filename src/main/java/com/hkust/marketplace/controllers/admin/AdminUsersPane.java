package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.AuditLogEntry;
import com.hkust.marketplace.models.ListingStatus;
import com.hkust.marketplace.models.Notification;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.models.UserRole;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SessionManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Task 3.3 – Manage All Users.
 *
 * <p>Lists buyer and seller accounts (admins are excluded) with search and role/status
 * filters.  Supports edit (full name), deactivate, reactivate, add and soft delete.
 * Deactivating hides a seller's live listings; soft-deleting also marks their listings
 * REMOVED and records an audit entry, while deals and ratings are retained.</p>
 */
final class AdminUsersPane extends VBox {

    private final AdminDashboardController host;
    private final DummyDataService data = DummyDataService.getInstance();

    private final TextField searchField = new TextField();
    private final ComboBox<String> roleFilter = new ComboBox<>(
            FXCollections.observableArrayList("All Roles", "Buyers", "Sellers"));
    private final ComboBox<String> statusFilter = new ComboBox<>(
            FXCollections.observableArrayList("All Statuses", "Active", "Deactivated"));
    private final Label summary = AdminUi.muted("");
    private final VBox list = new VBox(10);

    AdminUsersPane(AdminDashboardController host) {
        this(host, null);
    }

    AdminUsersPane(AdminDashboardController host, String initialSearch) {
        super(12);
        this.host = host;
        setPadding(new Insets(4));

        searchField.setPromptText("Search username or full name...");
        searchField.setPrefWidth(240);
        if (initialSearch != null) {
            searchField.setText(initialSearch);
        }
        roleFilter.getSelectionModel().selectFirst();
        statusFilter.getSelectionModel().selectFirst();

        Button auditButton = new Button("Audit Log");
        auditButton.getStyleClass().addAll("btn-outline", "btn-sm");
        auditButton.setOnAction(event -> showAuditLog());

        Button addButton = new Button("+ Add User");
        addButton.getStyleClass().addAll("btn-primary", "btn-sm");
        addButton.setOnAction(event -> addUser());

        HBox toolbar = AdminUi.toolbar(searchField, roleFilter, statusFilter,
                AdminUi.spacer(), auditButton, addButton);
        getChildren().addAll(toolbar, summary);

        list.setPadding(new Insets(2));
        ScrollPane scroll = AdminUi.scroll(list);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());
        roleFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());

        refresh();
    }

    private void refresh() {
        list.getChildren().clear();

        List<User> users = data.getAllUsers().stream()
                .filter(user -> user.getRole() != UserRole.ADMIN)
                .filter(this::matchesFilters)
                .sorted(Comparator
                        .comparingInt((User user) -> user.getRole() == UserRole.SELLER ? 1 : 0)
                        .thenComparing(User::getFullName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());

        summary.setText(users.size() + " account(s) shown"
                + (users.isEmpty() ? "" : " · admins are not managed here"));
        if (users.isEmpty()) {
            list.getChildren().add(AdminUi.emptyState("No matching users",
                    "Try clearing the search or filters. Soft-deleted accounts are hidden from this list."));
            return;
        }
        for (User user : users) {
            list.getChildren().add(createRow(user));
        }
    }

    private boolean matchesFilters(User user) {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (!query.isEmpty()
                && !user.getUsername().toLowerCase().contains(query)
                && !(user.getFullName() != null
                && user.getFullName().toLowerCase().contains(query))) {
            return false;
        }

        String role = roleFilter.getValue();
        if ("Buyers".equals(role) && user.getRole() != UserRole.BUYER) return false;
        if ("Sellers".equals(role) && user.getRole() != UserRole.SELLER) return false;

        String status = statusFilter.getValue();
        if ("Active".equals(status) && !user.isActive()) return false;
        if ("Deactivated".equals(status) && user.isActive()) return false;
        return true;
    }

    private HBox createRow(User user) {
        HBox row = new HBox(14);
        row.getStyleClass().add("card");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));

        Label avatar = new Label(AdminUi.initials(user.getFullName()));
        avatar.getStyleClass().add("avatar");

        Label name = new Label(user.getFullName() == null || user.getFullName().isBlank()
                ? "(no name)" : user.getFullName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label username = new Label("@" + user.getUsername());
        username.getStyleClass().add("muted");

        HBox badges = new HBox(6, AdminUi.roleBadge(user.getRole()),
                AdminUi.userStatusBadge(user.isActive()));

        long listingCount = user.getRole() == UserRole.SELLER
                ? data.getListingsBySeller(user.getUsername()).size() : 0;
        long dealCount = data.getAllDeals().stream()
                .filter(deal -> user.getUsername().equalsIgnoreCase(deal.getBuyerUsername())
                        || user.getUsername().equalsIgnoreCase(deal.getSellerUsername()))
                .count();
        Label meta = new Label("Joined " + AdminUi.formatDate(
                user.getCreatedAt() == null ? null : user.getCreatedAt().toLocalDate())
                + " · " + listingCount + " listing(s) · " + dealCount + " deal(s)");
        meta.getStyleClass().add("hint-text");

        VBox identity = new VBox(3, name, new HBox(8, username, badges), meta);
        HBox.setHgrow(identity, Priority.ALWAYS);

        Button edit = new Button("Edit");
        edit.getStyleClass().addAll("btn-outline", "btn-sm");
        edit.setOnAction(event -> editUser(user));

        Button toggle = new Button(user.isActive() ? "Deactivate" : "Reactivate");
        toggle.getStyleClass().addAll(user.isActive() ? "btn-danger" : "btn-success", "btn-sm");
        toggle.setOnAction(event -> {
            if (user.isActive()) {
                deactivateUser(user);
            } else {
                reactivateUser(user);
            }
        });

        Button delete = new Button("Delete");
        delete.getStyleClass().addAll("btn-danger", "btn-sm");
        delete.setOnAction(event -> deleteUser(user));

        HBox actions = new HBox(6, edit, toggle, delete);
        actions.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(avatar, identity, actions);
        com.hkust.marketplace.controllers.UiAnimations.hoverLift(row);
        return row;
    }

    private void addUser() {
        AdminUserFormDialog dialog = new AdminUserFormDialog(AdminUi.window(this), null);
        dialog.showAndWait().ifPresent(newUser -> {
            data.registerUser(newUser);
            data.addNotification(new Notification(newUser.getUsername(), "Account Created",
                    "An administrator created your account. You can now log in with your username.",
                    "Account"));
            data.addNotification(new Notification(adminUsername(), "User Created",
                    "@" + newUser.getUsername() + " (" + newUser.getRole() + ") was created.",
                    "Account", "USERS", newUser.getUsername()));
            data.logAudit(adminUsername(), "CREATE_USER", "USER", newUser.getUsername(),
                    "Created " + newUser.getRole() + " account");
            host.refreshUnreadBadge();
            refresh();
            AdminDialogs.info("User created",
                    "@" + newUser.getUsername() + " can now log in as a " + newUser.getRole() + ".");
        });
    }

    private void editUser(User user) {
        AdminUserFormDialog dialog = new AdminUserFormDialog(AdminUi.window(this), user);
        dialog.showAndWait().ifPresent(updated -> {
            User current = data.findUser(updated.getUsername());
            if (current == null || current.isDeleted()) {
                AdminDialogs.warn("User no longer available",
                        "The account was deleted while you were editing it. The list has been refreshed.");
                refresh();
                return;
            }
            data.updateUser(updated);
            data.addNotification(new Notification(adminUsername(), "User Updated",
                    "@" + updated.getUsername() + " was updated by an administrator.",
                    "Account", "USERS", updated.getUsername()));
            data.logAudit(adminUsername(), "EDIT_USER", "USER", updated.getUsername(),
                    "Updated full name");
            host.refreshUnreadBadge();
            refresh();
            AdminDialogs.info("User updated",
                    "@" + updated.getUsername() + "'s profile was updated.");
        });
    }

    private void deactivateUser(User user) {
        int liveListings = (int) data.getListingsBySeller(user.getUsername()).stream()
                .filter(listing -> listing.getStatus() == ListingStatus.LIVE)
                .count();
        String message = "Deactivate @" + user.getUsername() + "?\n\n"
                + "The user will not be able to log in until reactivated."
                + (liveListings > 0
                ? "\nTheir " + liveListings + " live listing(s) will be hidden from the marketplace "
                + "(they are retained and reappear after reactivation)."
                : "");
        if (!AdminDialogs.confirmDestructive("Deactivate account", message, "Deactivate")) {
            return;
        }
        if (data.deactivateUser(user.getUsername(), adminUsername())) {
            host.refreshUnreadBadge();
            refresh();
            AdminDialogs.info("Account deactivated",
                    "@" + user.getUsername() + " can no longer log in.");
        }
    }

    private void reactivateUser(User user) {
        if (!AdminDialogs.confirmDestructive("Reactivate account",
                "Reactivate @" + user.getUsername() + "?\n\nThe user will be able to log in "
                        + "and their live listings will be visible again.",
                "Reactivate")) {
            return;
        }
        if (data.reactivateUser(user.getUsername(), adminUsername())) {
            host.refreshUnreadBadge();
            refresh();
            AdminDialogs.info("Account reactivated",
                    "@" + user.getUsername() + " can log in again.");
        }
    }

    private void deleteUser(User user) {
        long listingCount = data.getListingsBySeller(user.getUsername()).size();
        long dealCount = data.getAllDeals().stream()
                .filter(deal -> user.getUsername().equalsIgnoreCase(deal.getBuyerUsername())
                        || user.getUsername().equalsIgnoreCase(deal.getSellerUsername()))
                .count();
        long ratingCount = data.getAllRatings().stream()
                .filter(rating -> user.getUsername().equalsIgnoreCase(rating.getSellerUsername())
                        || user.getUsername().equalsIgnoreCase(rating.getBuyerUsername()))
                .count();
        long chatCount = data.getChatsForBuyer(user.getUsername()).size()
                + data.getChatsForSeller(user.getUsername()).size();

        String message = "Permanently delete @" + user.getUsername() + "?\n\n"
                + "This performs a soft delete:\n"
                + "• the account is marked deleted and can never log in again;\n"
                + "• " + listingCount + " listing(s) are set to Removed and hidden;\n"
                + "• active requests/deals are cancelled and counterparties are notified;\n"
                + "• " + dealCount + " deal(s), " + ratingCount + " rating(s) and "
                + chatCount + " chat(s) are retained for audit;\n"
                + "• an audit-log entry records who deleted the account and when.\n\n"
                + "The account cannot be restored from this screen.";
        if (!AdminDialogs.confirmDestructive("Delete account", message, "Delete account")) {
            return;
        }
        if (data.deleteUser(user.getUsername(), adminUsername())) {
            host.refreshUnreadBadge();
            refresh();
            AdminDialogs.info("Account deleted",
                    "@" + user.getUsername() + " has been soft-deleted. "
                            + "Deals, ratings and chats remain available for audit.");
        }
    }

    private void showAuditLog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Admin Audit Log");
        dialog.setHeaderText("Recorded administrative actions");
        dialog.initOwner(AdminUi.window(this));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<AuditLogEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(AdminUi.emptyState("No audit entries yet",
                "Approvals, take-downs and account changes are recorded here."));

        TableColumn<AuditLogEntry, String> time = new TableColumn<>("Time");
        time.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                AdminUi.formatDateTime(cell.getValue().getTimestamp())));
        TableColumn<AuditLogEntry, String> actor = new TableColumn<>("Admin");
        actor.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getActorUsername()));
        TableColumn<AuditLogEntry, String> action = new TableColumn<>("Action");
        action.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getAction()));
        TableColumn<AuditLogEntry, String> target = new TableColumn<>("Target");
        target.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getTargetType() + " " + cell.getValue().getTargetId()));
        TableColumn<AuditLogEntry, String> details = new TableColumn<>("Details");
        details.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getDetails()));

        table.getColumns().addAll(time, actor, action, target, details);
        table.setItems(FXCollections.observableArrayList(data.getAuditLog()));
        table.setPrefSize(880, 460);

        dialog.getDialogPane().setContent(table);
        dialog.getDialogPane().setPrefWidth(920);
        dialog.showAndWait();
    }

    private String adminUsername() {
        User user = SessionManager.getCurrentUser();
        return user == null ? DummyDataService.ADMIN_USERNAME : user.getUsername();
    }
}
