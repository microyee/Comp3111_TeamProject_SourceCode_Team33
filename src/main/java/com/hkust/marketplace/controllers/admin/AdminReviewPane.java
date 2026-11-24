package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Listing;
import com.hkust.marketplace.models.ListingStatus;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Task 3.2 – Listing Review & Approval.
 *
 * <p>Shows every submission with status PENDING_REVIEW, including seller identity,
 * category, type, meet-up location, submitted date, status and photos.  Approving
 * publishes the listing; rejecting requires a written reason that is stored and sent
 * to the seller.  The listing is re-read before each decision so a concurrent change
 * cannot be overwritten.</p>
 */
final class AdminReviewPane extends VBox {

    private final AdminDashboardController host;
    private final DummyDataService data = DummyDataService.getInstance();
    private final Label summary = AdminUi.muted("");
    private final VBox list = new VBox(12);

    AdminReviewPane(AdminDashboardController host) {
        super(12);
        this.host = host;
        setPadding(new Insets(4));

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().addAll("btn-outline", "btn-sm");
        refreshButton.setOnAction(event -> refresh());

        HBox header = new HBox(12, summary, AdminUi.spacer(), refreshButton);
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
        List<Listing> pending = data.getPendingListings();
        summary.setText(pending.isEmpty()
                ? "No submissions waiting for review."
                : pending.size() + " submission(s) waiting for review");
        if (pending.isEmpty()) {
            list.getChildren().add(AdminUi.emptyState("Nothing to review",
                    "When a seller submits a new listing it will appear here for approval."));
            return;
        }
        for (Listing listing : pending) {
            list.getChildren().add(createCard(listing));
        }
    }

    private VBox createCard(Listing listing) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        Label title = new Label(listing.getTitle());
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        HBox titleRow = new HBox(10, title, AdminUi.spacer(), AdminUi.statusBadge(listing.getStatus()));
        titleRow.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(6);
        int row = 0;
        addDetail(grid, row++, "Seller", listing.getSellerFullName()
                + " (@" + listing.getSellerUsername() + ")");
        addDetail(grid, row++, "Category", listing.getCategory());
        addDetail(grid, row++, "Listing type", listing.getTypeDisplay());
        addDetail(grid, row++, "Condition", listing.getCondition());
        addDetail(grid, row++, "Meet-up location", listing.getMeetupLocation());
        addDetail(grid, row++, "Price / duration", priceAndDuration(listing));
        addDetail(grid, row++, "Submitted", AdminUi.formatDateTime(listing.getPostedAt()));
        addDetail(grid, row++, "Status", AdminUi.statusText(listing.getStatus()));
        addDetail(grid, row, "Photos", listing.getPhotoPaths().isEmpty()
                ? "No photos uploaded" : listing.getPhotoPaths().size() + " photo(s)");

        Label descriptionTitle = AdminUi.fieldLabel("Description");
        Label description = new Label(listing.getDescription() == null
                || listing.getDescription().isBlank()
                ? "No description provided." : listing.getDescription());
        description.setWrapText(true);
        description.getStyleClass().add("muted");

        FlowPane photos = new FlowPane(8, 8);
        if (listing.getPhotoPaths().isEmpty()) {
            photos.getChildren().add(AdminUi.muted("No photos uploaded."));
        } else {
            for (String path : listing.getPhotoPaths()) {
                photos.getChildren().add(AdminUi.photoThumb(path, 110, 76));
            }
        }

        Button approve = new Button("Approve");
        approve.getStyleClass().addAll("btn-success", "btn-sm");
        approve.setOnAction(event -> approve(listing));

        Button reject = new Button("Reject");
        reject.getStyleClass().addAll("btn-danger", "btn-sm");
        reject.setOnAction(event -> reject(listing));

        HBox actions = new HBox(10, approve, reject);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(titleRow, grid, descriptionTitle, description,
                AdminUi.fieldLabel("Photos"), photos, actions);
        return card;
    }

    private String priceAndDuration(Listing listing) {
        if (listing.getType() == null) return "—";
        return switch (listing.getType()) {
            case SELL -> listing.getPrice() == null ? "Price not set" : listing.getPriceDisplay();
            case LEND -> listing.getLendingDays() == null
                    ? "Duration not set" : listing.getLendingDays() + " days";
            case GIVEAWAY -> "Free / giveaway";
        };
    }

    private void addDetail(GridPane grid, int row, String label, String value) {
        Label key = new Label(label);
        key.setStyle("-fx-font-weight: 600; -fx-text-fill: #5a5a7a; -fx-font-size: 12px;");
        Label val = new Label(value == null || value.isBlank() ? "—" : value);
        val.setWrapText(true);
        grid.add(key, 0, row);
        grid.add(val, 1, row);
    }

    private void approve(Listing listing) {
        if (!AdminDialogs.confirm("Approve listing?",
                "Publish '" + listing.getTitle() + "' by @" + listing.getSellerUsername()
                        + " to the buyer marketplace?")) {
            return;
        }
        Listing current = data.findListing(listing.getId());
        if (current == null || current.getStatus() != ListingStatus.PENDING_REVIEW) {
            AdminDialogs.warn("Listing already processed",
                    "This listing is no longer pending review. The list has been refreshed.");
            refresh();
            return;
        }
        try {
            if (data.approveListing(current.getId(), adminUsername())) {
                String visibility = data.catalog().isPublic(current) ? " is now live."
                        : " will be visible when the seller account is active.";
                AdminDialogs.info("Listing approved",
                        "'" + current.getTitle() + "'" + visibility + " The seller has been notified.");
            } else {
                AdminDialogs.warn("Could not approve",
                        "The listing changed while you were reviewing it. The list has been refreshed.");
            }
        } catch (RuntimeException ex) {
            AdminDialogs.error("Could not save the approval",
                    "The listing remains Pending Review. " + ex.getMessage());
        }
        refresh();
    }

    private void reject(Listing listing) {
        Optional<String> reason = AdminDialogs.promptReason(
                "Reject '" + listing.getTitle() + "'",
                "Tell the seller why this submission cannot be published. "
                        + "The reason is stored and included in the seller's notification.",
                "Rejection reason (required)");
        if (reason.isEmpty()) {
            return;
        }

        Listing current = data.findListing(listing.getId());
        if (current == null || current.getStatus() != ListingStatus.PENDING_REVIEW) {
            AdminDialogs.warn("Listing already processed",
                    "This listing is no longer pending review. The list has been refreshed.");
            refresh();
            return;
        }
        try {
            if (data.rejectListing(current.getId(), reason.get(), adminUsername())) {
                AdminDialogs.info("Listing rejected",
                        "The reason was saved and sent to @" + current.getSellerUsername() + ".");
            } else {
                AdminDialogs.warn("Could not reject",
                        "The listing changed while you were reviewing it. The list has been refreshed.");
            }
        } catch (RuntimeException ex) {
            AdminDialogs.error("Could not save the rejection",
                    "The listing remains Pending Review. " + ex.getMessage());
        }
        refresh();
    }

    private String adminUsername() {
        User user = SessionManager.getCurrentUser();
        return user == null ? DummyDataService.ADMIN_USERNAME : user.getUsername();
    }
}
