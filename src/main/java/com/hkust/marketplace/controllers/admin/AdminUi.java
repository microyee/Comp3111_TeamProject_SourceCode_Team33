package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.ListingStatus;
import com.hkust.marketplace.models.UserRole;
import com.hkust.marketplace.utils.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Small presentation helpers shared by the admin screens.  Keeping these in one place
 * keeps status colours, date formats and spacing consistent across Task 3.
 */
final class AdminUi {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private AdminUi() {
    }

    static Label title(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        return label;
    }

    static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: 600; -fx-text-fill: #1a1a2e; -fx-font-size: 13px;");
        return label;
    }

    static Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }

    static Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("hint-text");
        label.setWrapText(true);
        return label;
    }

    static Label errorLabel() {
        Label label = new Label();
        label.getStyleClass().add("error-text");
        label.setWrapText(true);
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    static Label successLabel() {
        Label label = new Label();
        label.getStyleClass().add("success-text");
        label.setWrapText(true);
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    static void setMessage(Label label, String message, boolean error) {
        label.setText(message == null ? "" : message);
        label.setVisible(message != null && !message.isBlank());
        label.setManaged(message != null && !message.isBlank());
        label.getStyleClass().removeAll("error-text", "success-text");
        label.getStyleClass().add(error ? "error-text" : "success-text");
    }

    static void clear(Label... labels) {
        for (Label label : labels) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    static Label badge(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add("badge");
        if (styleClass != null) {
            label.getStyleClass().add(styleClass);
        }
        return label;
    }

    static Label statusBadge(ListingStatus status) {
        return badge(statusText(status), statusStyle(status));
    }

    static String statusText(ListingStatus status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case PENDING_REVIEW -> "Pending Review";
            case LIVE -> "Live";
            case REJECTED -> "Rejected";
            case RESERVED -> "Reserved";
            case SOLD -> "Sold";
            case LENT -> "Lent";
            case GIVEN -> "Given";
            case REMOVED -> "Removed";
        };
    }

    static String statusStyle(ListingStatus status) {
        if (status == null) return "badge-removed";
        return switch (status) {
            case LIVE -> "badge-live";
            case PENDING_REVIEW -> "badge-pending";
            case REJECTED -> "badge-rejected";
            case RESERVED -> "badge-reserved";
            case SOLD -> "badge-sold";
            case LENT -> "badge-lent";
            case GIVEN -> "badge-given";
            case REMOVED -> "badge-removed";
        };
    }

    /** Compact status label for dense tables. */
    static String statusShortText(ListingStatus status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case PENDING_REVIEW -> "Pending";
            case LIVE -> "Live";
            case REJECTED -> "Rejected";
            case RESERVED -> "Reserved";
            case SOLD -> "Sold";
            case LENT -> "Lent";
            case GIVEN -> "Given";
            case REMOVED -> "Removed";
        };
    }

    static Label roleBadge(UserRole role) {
        if (role == null) return badge("Unknown", null);
        return switch (role) {
            case BUYER -> badge("Buyer", "badge-sold");
            case SELLER -> badge("Seller", "badge-reserved");
            case ADMIN -> badge("Admin", "badge-live");
        };
    }

    static Label userStatusBadge(boolean active) {
        return active ? badge("Active", "badge-active") : badge("Deactivated", "badge-inactive");
    }

    static Label dealStatusBadge(String status) {
        String value = status == null ? "Unknown" : status;
        String style = switch (value.toLowerCase()) {
            case "pending" -> "badge-pending";
            case "accepted" -> "badge-live";
            case "completed" -> "badge-sold";
            case "returned" -> "badge-removed";
            case "cancelled" -> "badge-rejected";
            default -> null;
        };
        return badge(value, style);
    }

    static Label categoryBadge(String category) {
        String style = switch (category == null ? "" : category) {
            case "Submission" -> "badge-pending";
            case "Report" -> "badge-rejected";
            case "Account" -> "badge-sold";
            case "Offer" -> "badge-reserved";
            default -> "badge-live";
        };
        return badge(category == null ? "System" : category, style);
    }

    static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    static HBox toolbar(Node... nodes) {
        HBox box = new HBox(10, nodes);
        box.getStyleClass().add("toolbar");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12));
        return box;
    }

    static VBox emptyState(String heading, String message) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50, 20, 50, 20));
        box.getStyleClass().add("card");
        Label icon = new Label("◎");
        icon.setStyle("-fx-font-size: 34px; -fx-text-fill: #A68137;");
        Label title = title(heading);
        Label detail = muted(message);
        detail.setWrapText(true);
        detail.setStyle("-fx-text-alignment: center;");
        detail.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        box.getChildren().addAll(icon, title, detail);
        return box;
    }

    static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        return scroll;
    }

    static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    static String formatDate(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "—" : DATE_TIME.format(dateTime);
    }

    /**
     * Builds a thumbnail for a stored photo path.  Photo files are user-selected local
     * files in this demo, so missing/unreadable files fall back to a neutral tile.
     */
    static Node photoThumb(String path, double width, double height) {
        StackPane tile = new StackPane();
        tile.setPrefSize(width, height);
        tile.setMinSize(width, height);
        tile.setStyle("-fx-background-color: #eef2f7; -fx-background-radius: 8; -fx-padding: 3;");

        if (path == null || path.isBlank() || !new File(path).isFile()) {
            Label missing = new Label("No image");
            missing.setStyle("-fx-text-fill: #9aa0b0; -fx-font-size: 10px;");
            tile.getChildren().add(missing);
            return tile;
        }

        Image image = new Image(new File(path).toURI().toString(), width, height, true, true, true);
        if (image.isError()) {
            Label missing = new Label("Unreadable");
            missing.setStyle("-fx-text-fill: #9aa0b0; -fx-font-size: 10px;");
            tile.getChildren().add(missing);
            return tile;
        }

        ImageView view = new ImageView(image);
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        tile.getChildren().add(view);
        return tile;
    }

    static Window window(Node node) {
        if (node != null && node.getScene() != null) {
            return node.getScene().getWindow();
        }
        return SceneManager.getPrimaryStage();
    }
}
