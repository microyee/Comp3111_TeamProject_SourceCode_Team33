package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Notification;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Task 3.6 – Admin Notification Board.
 *
 * <p>Notifications are newest-first, timestamped and categorised (Submission, Report,
 * Account, System).  Unread items are highlighted.  Opening an item shows its details,
 * marks it read and offers navigation to the related screen.  If the related listing or
 * account no longer exists, the stored details are still shown with an explanation.</p>
 */
final class AdminNotificationsPane extends VBox {

    private final AdminDashboardController host;
    private final DummyDataService data = DummyDataService.getInstance();
    private final Label summary = AdminUi.muted("");
    private final VBox list = new VBox(10);

    AdminNotificationsPane(AdminDashboardController host) {
        super(12);
        this.host = host;
        setPadding(new Insets(4));

        Button markAll = new Button("Mark all as read");
        markAll.getStyleClass().addAll("btn-outline", "btn-sm");
        markAll.setOnAction(event -> {
            data.markAllNotificationsRead(adminUsername());
            host.refreshUnreadBadge();
            refresh();
        });

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().addAll("btn-outline", "btn-sm");
        refreshButton.setOnAction(event -> refresh());

        HBox header = new HBox(12, summary, AdminUi.spacer(), refreshButton, markAll);
        header.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(header);

        list.setPadding(new Insets(2));
        ScrollPane scroll = AdminUi.scroll(list);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        refresh();
    }

    private void refresh() {
        list.getChildren().clear();
        List<Notification> notifications = data.getNotificationsFor(adminUsername());
        int unread = data.getUnreadCount(adminUsername());
        summary.setText(notifications.isEmpty()
                ? "No notifications."
                : notifications.size() + " notification(s) · " + unread + " unread");

        if (notifications.isEmpty()) {
            list.getChildren().add(AdminUi.emptyState("No notifications",
                    "New submissions, reports and account changes will appear here."));
            return;
        }
        for (Notification notification : notifications) {
            list.getChildren().add(createRow(notification));
        }
    }

    private HBox createRow(Notification notification) {
        HBox row = new HBox(12);
        row.getStyleClass().addAll("notif-item",
                notification.isRead() ? "notif-read" : "notif-unread");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(13, 16, 13, 16));

        Label unreadDot = new Label(notification.isRead() ? "" : "●");
        unreadDot.setStyle("-fx-text-fill: #A68137; -fx-font-size: 12px;");

        Label title = new Label(notification.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label message = new Label(notification.getMessage());
        message.setWrapText(true);
        message.getStyleClass().add("muted");
        Label meta = new Label(notification.getCategory() + " · "
                + AdminUi.formatDateTime(notification.getTimestamp()));
        meta.getStyleClass().add("hint-text");

        VBox body = new VBox(4, title, message, meta);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox badges = new HBox(6, AdminUi.categoryBadge(notification.getCategory()));
        if (notification.hasRelatedTarget()) {
            Button open = new Button("Open");
            open.getStyleClass().addAll("btn-primary", "btn-sm");
            open.setOnAction(event -> open(notification));
            badges.getChildren().add(open);
        } else {
            Button details = new Button("Details");
            details.getStyleClass().addAll("btn-outline", "btn-sm");
            details.setOnAction(event -> open(notification));
            badges.getChildren().add(details);
        }
        badges.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(unreadDot, body, badges);
        return row;
    }

    private void open(Notification notification) {
        if (!notification.isRead()) {
            data.markNotificationRead(notification.getId());
        }
        host.refreshUnreadBadge();
        refresh();

        boolean available = relatedTargetAvailable(notification);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(notification.getTitle());
        dialog.setHeaderText(notification.getTitle());
        dialog.initOwner(AdminUi.window(this));

        ButtonType close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType openRelated = new ButtonType("Open related screen",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(close);
        if (notification.hasRelatedTarget() && available) {
            dialog.getDialogPane().getButtonTypes().add(openRelated);
        }

        Label category = AdminUi.categoryBadge(notification.getCategory());
        Label message = new Label(notification.getMessage());
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #1a1a2e;");
        Label meta = new Label("Received " + AdminUi.formatDateTime(notification.getTimestamp()));
        meta.getStyleClass().add("hint-text");

        VBox content = new VBox(10, category, message, meta);
        content.setPadding(new Insets(8));
        content.setPrefWidth(440);

        if (notification.hasRelatedTarget()) {
            Label related = new Label(available
                    ? "Related item: " + notification.getRelatedScreen()
                    + " · " + notification.getRelatedId()
                    : "The related listing or account is no longer available. "
                    + "The notification details above are retained.");
            related.setWrapText(true);
            related.getStyleClass().add(available ? "hint-text" : "error-text");
            content.getChildren().add(related);
        }

        dialog.getDialogPane().setContent(content);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == openRelated) {
            host.openNotificationTarget(notification.getRelatedScreen(),
                    notification.getRelatedId());
        }
    }

    private boolean relatedTargetAvailable(Notification notification) {
        String screen = notification.getRelatedScreen();
        if (screen == null) return true;
        String id = notification.getRelatedId();
        return switch (screen) {
            case "LISTINGS" -> data.findListing(id) != null;
            case "USERS" -> {
                User user = data.findUser(id);
                yield user != null && !user.isDeleted();
            }
            case "DEALS" -> data.findDeal(id) != null;
            case "RATINGS" -> data.findRating(id) != null;
            default -> true;
        };
    }

    private String adminUsername() {
        User user = SessionManager.getCurrentUser();
        return user == null ? DummyDataService.ADMIN_USERNAME : user.getUsername();
    }
}
