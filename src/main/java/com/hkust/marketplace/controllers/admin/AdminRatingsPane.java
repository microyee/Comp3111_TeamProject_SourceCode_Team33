package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Rating;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Task 3.8 – Seller Ratings Overview.
 *
 * <p>Lists every rating (including hidden ones) with seller/buyer, score, comment,
 * date and visibility.  Filters cover user, score and date range.  Hiding a review
 * removes it from the public seller average; unhiding restores it.</p>
 */
final class AdminRatingsPane extends VBox {

    private final DummyDataService data = DummyDataService.getInstance();

    private final TextField searchField = new TextField();
    private final ComboBox<String> ratingFilter = new ComboBox<>();
    private final ComboBox<String> visibilityFilter = new ComboBox<>(
            FXCollections.observableArrayList("All reviews", "Visible only", "Hidden only"));
    private final DatePicker fromDate = new DatePicker();
    private final DatePicker toDate = new DatePicker();
    private final Label summary = AdminUi.muted("");
    private final Label errorLabel = AdminUi.errorLabel();
    private final VBox list = new VBox(10);

    AdminRatingsPane(AdminDashboardController host, String initialSearch) {
        super(10);
        setPadding(new Insets(4));

        searchField.setPromptText("Search seller, buyer or listing...");
        searchField.setPrefWidth(220);
        if (initialSearch != null) {
            searchField.setText(initialSearch);
        }

        ratingFilter.getItems().add("All ratings");
        for (int score = 5; score >= 1; score--) {
            ratingFilter.getItems().add(score + " stars");
        }
        ratingFilter.getSelectionModel().selectFirst();
        visibilityFilter.getSelectionModel().selectFirst();

        fromDate.setPromptText("From");
        fromDate.setPrefWidth(120);
        toDate.setPromptText("To");
        toDate.setPrefWidth(120);

        Button clear = new Button("Clear");
        clear.getStyleClass().addAll("btn-outline", "btn-sm");
        clear.setOnAction(event -> {
            searchField.clear();
            ratingFilter.getSelectionModel().selectFirst();
            visibilityFilter.getSelectionModel().selectFirst();
            fromDate.setValue(null);
            toDate.setValue(null);
            refresh();
        });

        HBox toolbar = AdminUi.toolbar(searchField, ratingFilter, visibilityFilter,
                fromDate, toDate, clear);
        getChildren().addAll(toolbar, summary, errorLabel);

        list.setPadding(new Insets(2));
        ScrollPane scroll = AdminUi.scroll(list);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());
        ratingFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        visibilityFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        fromDate.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        toDate.valueProperty().addListener((observable, oldValue, newValue) -> refresh());

        refresh();
    }

    private void refresh() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from != null && to != null && from.isAfter(to)) {
            AdminUi.setMessage(errorLabel,
                    "Invalid date range: the start date must be on or before the end date.", true);
            return;
        }
        AdminUi.clear(errorLabel);

        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String ratingValue = ratingFilter.getValue();
        String visibility = visibilityFilter.getValue();

        List<Rating> filtered = data.getAllRatings().stream()
                .filter(review -> matchesQuery(review, query))
                .filter(review -> ratingValue == null || "All ratings".equals(ratingValue)
                        || ratingValue.startsWith(String.valueOf(review.getRating())))
                .filter(review -> {
                    if ("Visible only".equals(visibility)) return !review.isHidden();
                    if ("Hidden only".equals(visibility)) return review.isHidden();
                    return true;
                })
                .filter(review -> {
                    LocalDate date = review.getCreatedAt() == null
                            ? null : review.getCreatedAt().toLocalDate();
                    if (date == null) return from == null && to == null;
                    if (from != null && date.isBefore(from)) return false;
                    if (to != null && date.isAfter(to)) return false;
                    return true;
                })
                .sorted(Comparator.comparing(Rating::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        list.getChildren().clear();
        summary.setText(filtered.size() + " rating(s) shown"
                + " · hidden ratings are excluded from public averages");
        if (filtered.isEmpty()) {
            list.getChildren().add(AdminUi.emptyState("No matching ratings",
                    "Clear the filters to see all buyer ratings."));
            return;
        }
        for (Rating review : filtered) {
            list.getChildren().add(createCard(review));
        }
    }

    private boolean matchesQuery(Rating review, String query) {
        if (query.equalsIgnoreCase(review.getId())) return true;
        if (query.isEmpty()) return true;
        return contains(review.getSellerUsername(), query)
                || contains(review.getBuyerUsername(), query)
                || contains(review.getListingTitle(), query)
                || contains(review.getComment(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private VBox createCard(Rating review) {
        VBox card = new VBox(7);
        card.getStyleClass().add("card");
        if (review.isHidden()) {
            card.getStyleClass().add("hidden-review");
        }
        card.setPadding(new Insets(14));

        Label stars = new Label("★".repeat(review.getRating())
                + "☆".repeat(5 - review.getRating()));
        stars.setStyle("-fx-text-fill: #A68137; -fx-font-size: 17px;");

        HBox top = new HBox(10, stars, AdminUi.spacer());
        top.setAlignment(Pos.CENTER_LEFT);
        if (review.isHidden()) {
            top.getChildren().add(AdminUi.badge("Hidden", "badge-rejected"));
        }
        top.getChildren().add(AdminUi.badge(review.getRating() + " / 5", "badge-sold"));

        Label parties = new Label("Seller: @" + review.getSellerUsername()
                + "  ·  Buyer: @" + review.getBuyerUsername());
        parties.setStyle("-fx-font-weight: 600;");

        Label listing = new Label("Listing: " + (review.getListingTitle() == null
                ? "—" : review.getListingTitle()));
        listing.getStyleClass().add("muted");

        Label comment = new Label(review.getComment() == null || review.getComment().isBlank()
                ? "(no written comment)" : review.getComment());
        comment.setWrapText(true);
        comment.setStyle("-fx-text-fill: #1a1a2e;");

        VBox details = new VBox(5, parties, listing, comment);

        if (review.getSellerReply() != null && !review.getSellerReply().isBlank()) {
            Label reply = new Label("Seller reply: " + review.getSellerReply());
            reply.setWrapText(true);
            reply.setStyle("-fx-background-color: #f5f7fa; -fx-padding: 8; "
                    + "-fx-background-radius: 6; -fx-text-fill: #5a5a7a;");
            details.getChildren().add(reply);
        }

        double publicAverage = data.getPublicAverageRating(review.getSellerUsername());
        int publicCount = data.getPublicRatingCount(review.getSellerUsername());
        Label publicMeta = new Label(String.format(
                "Public seller average: %.1f★ from %d visible review(s)", publicAverage, publicCount));
        publicMeta.getStyleClass().add("hint-text");

        Label date = new Label(AdminUi.formatDate(review.getCreatedAt() == null
                ? null : review.getCreatedAt().toLocalDate()));
        date.getStyleClass().add("hint-text");

        Button toggle = new Button(review.isHidden() ? "Unhide" : "Hide");
        toggle.getStyleClass().addAll(review.isHidden() ? "btn-success" : "btn-danger", "btn-sm");
        toggle.setOnAction(event -> moderate(review));

        HBox footer = new HBox(10, date, publicMeta, AdminUi.spacer(), toggle);
        footer.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(top, details, footer);
        com.hkust.marketplace.controllers.UiAnimations.hoverLift(card);
        return card;
    }

    private void moderate(Rating review) {
        boolean hiding = !review.isHidden();
        String action = hiding ? "Hide rating" : "Unhide rating";
        String message = hiding
                ? "Hide this " + review.getRating() + "-star rating from @"
                + review.getBuyerUsername() + "?\n\nIt will no longer be publicly displayed "
                + "or counted in the seller's public average, but it is retained for audit."
                : "Make this rating visible again and include it in the seller's public average?";
        if (!AdminDialogs.confirm(action, message)) {
            return;
        }

        review.setHidden(hiding);
        data.updateRating(review);
        data.recalculateSellerRating(review.getSellerUsername());
        data.logAudit(adminUsername(), hiding ? "HIDE_REVIEW" : "UNHIDE_REVIEW",
                "REVIEW", review.getId(),
                "Seller @" + review.getSellerUsername() + ", " + review.getRating() + " stars");
        refresh();
    }

    private String adminUsername() {
        User user = SessionManager.getCurrentUser();
        return user == null ? DummyDataService.ADMIN_USERNAME : user.getUsername();
    }
}
