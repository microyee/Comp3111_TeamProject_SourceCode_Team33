package com.hkust.marketplace.controllers.seller;

import com.hkust.marketplace.MarketplaceApp;
import com.hkust.marketplace.models.*;
import com.hkust.marketplace.utils.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.hkust.marketplace.controllers.PortalUi.*;

public class SellerDashboardController {
    @FXML private StackPane contentPane;
    @FXML private Label pageTitle, welcomeLabel, unreadLabel;
    @FXML private Button btnListings, btnCreate, btnRequests, btnChat, btnStats, btnReviews, btnNotifications, btnProfile;

    private final DummyDataService data = DummyDataService.getInstance();
    private final MarketplaceAccounts accounts = new MarketplaceAccounts(data);
    private final MarketplaceActivity activity = data.activity();
    private final SellerMarketplace marketplace = new SellerMarketplace(data);
    private User seller;
    private Button active;
    private Runnable onLoanUpdate = () -> {};

    @FXML
    private void initialize() {
        if (!SessionManager.isSessionValid() || !SessionManager.isSeller()) {
            SessionManager.logout();
            Platform.runLater(SceneManager::showSellerLogin);
            return;
        }
        seller = SessionManager.getCurrentUser();
        welcome();
        contentPane.addEventHandler(MarketplaceApp.LOANS_UPDATED, event -> {
            badge();
            onLoanUpdate.run();
        });
        showMyListings();
    }

    private void welcome() {
        welcomeLabel.setText("Seller: " + seller.getFullName());
    }

    private void badge() {
        int count = data.getUnreadCount(seller.getUsername());
        unreadLabel.setText(count + " unread");
        unreadLabel.getStyleClass().setAll("badge", count == 0 ? "badge-live" : "badge-pending");
    }

    private void show(Button button, String title, Node page) {
        if (active != null) active.getStyleClass().remove("sidebar-btn-active");
        button.getStyleClass().remove("sidebar-btn-active");
        button.getStyleClass().add("sidebar-btn-active");
        active = button;
        pageTitle.setText(title);
        contentPane.getChildren().setAll(page);
        onLoanUpdate = () -> {};
        badge();
    }

    @FXML
    private void showMyListings() { showMyListings(null); }

    private void showMyListings(String highlight) {
        var status = choice("All statuses", List.of(ListingStatus.values()));
        TextField query = field("Search my listings");
        VBox rows = new VBox(10);
        Runnable render = () -> {
            rows.getChildren().clear();
            String text = query.getText().strip().toLowerCase(Locale.ROOT);
            for (Listing listing : data.getListingsBySeller(seller.getUsername())) {
                if (!MarketplaceCatalog.contains(listing.getTitle(), text)
                        || status.getValue() != null && status.getValue() != listing.getStatus()) continue;
                if (listing.getStatus() == ListingStatus.REMOVED && seller.getUsername().equals(listing.getRemovedBy())
                        && status.getValue() != ListingStatus.REMOVED && !listing.getId().equals(highlight)) continue;
                rows.getChildren().add(listingRow(listing, listing.getId().equals(highlight), this::showMyListings));
            }
            if (rows.getChildren().isEmpty()) rows.getChildren().add(label("No listings match these filters.", "muted"));
        };
        query.textProperty().addListener((observable, old, value) -> render.run());
        status.setOnAction(event -> render.run());
        HBox toolbar = new HBox(8, query, status, button("Clear filters", "btn-outline", () -> {
            query.clear();
            status.setValue(null);
        }), button("Create listing", "btn-primary", this::showCreateListing));
        toolbar.getStyleClass().add("toolbar");
        ScrollPane list = scroll(rows);
        VBox.setVgrow(list, Priority.ALWAYS);
        show(btnListings, "My Listings", new VBox(12, toolbar, list));
        onLoanUpdate = render;
        render.run();
    }

    private Node listingRow(Listing listing, boolean highlight, Runnable refresh) {
        String id = listing.getId();
        VBox row = card(label(listing.getTitle(), "section-title"),
                label(listing.getTypeDisplay() + " · " + listing.getPriceDisplay() + " · " + listing.getCategory()
                        + " · " + listing.getCondition(), "label"),
                label(listing.getStatus() + " · " + listing.getMeetupLocation() + " · " + listing.getViewCount() + " views", "muted"));
        if (highlight) row.getStyleClass().add("notif-unread");
        if (listing.getRejectionReason() != null) row.getChildren().add(label("Review reason: " + listing.getRejectionReason(), "error-text"));
        if (listing.getRemovalReason() != null) row.getChildren().add(label("Removal reason: " + listing.getRemovalReason(), "muted"));
        HBox actions = new HBox(8, button("Incoming requests", "btn-outline", () -> showRequests(id, null)));
        if (marketplace.canEdit(seller, id)) {
            actions.getChildren().addAll(button("Edit", "btn-outline", () -> editListing(listing)),
                    button("Delete", "btn-danger", () -> {
                        if (confirm(contentPane, "Delete listing?", listing.getTitle())) {
                            marketplace.remove(seller, id);
                            refresh.run();
                        }
                    }));
        }
        if (marketplace.canMarkSold(seller, id)) {
            actions.getChildren().add(button("Mark as sold", "btn-success", () -> {
                if (confirm(contentPane, "Mark as sold?", listing.getTitle())) {
                    marketplace.markSold(seller, id);
                    refresh.run();
                }
            }));
        }
        row.getChildren().add(actions);
        return row;
    }

    @FXML
    private void showCreateListing() {
        show(btnCreate, "Create Listing", SellerListingForm.build(seller, null, marketplace, this::showMyListings));
    }

    private void editListing(Listing listing) {
        if (!marketplace.canEdit(seller, listing.getId())) throw new IllegalArgumentException("This listing can no longer be edited.");
        show(btnListings, "Edit Listing", SellerListingForm.build(seller, listing, marketplace, this::showMyListings));
    }

    @FXML
    private void showRequests() { showRequests(null, null); }

    private void showRequests(String listingId, String requestId) {
        VBox rows = new VBox(10);
        CheckBox activeOnly = new CheckBox("Open requests only");
        activeOnly.setSelected(requestId == null);
        Runnable render = () -> {
            rows.getChildren().clear();
            List<Request> requests = data.getRequestsForSeller(seller.getUsername()).stream()
                    .filter(r -> listingId == null || listingId.equals(r.getListingId()))
                    .filter(r -> !activeOnly.isSelected() || r.isActive())
                    .sorted(Comparator.comparing(Request::getCreatedAt).reversed()).toList();
            for (Request request : requests) {
                VBox row = requestRow(request, () -> showRequests(listingId, request.getId()));
                if (request.getId().equals(requestId)) row.getStyleClass().add("notif-unread");
                rows.getChildren().add(row);
            }
            if (rows.getChildren().isEmpty()) rows.getChildren().add(label("No matching requests.", "muted"));
        };
        HBox toolbar = new HBox(8, activeOnly);
        if (listingId != null) toolbar.getChildren().add(button("Show all listings", "btn-outline", this::showRequests));
        toolbar.getStyleClass().add("toolbar");
        activeOnly.setOnAction(event -> render.run());
        ScrollPane list = scroll(rows);
        VBox.setVgrow(list, Priority.ALWAYS);
        show(btnRequests, "Incoming Requests", new VBox(12, toolbar, list));
        onLoanUpdate = render;
        render.run();
    }

    private VBox requestRow(Request request, Runnable refresh) {
        var terms = MarketplaceActivity.proposed(request);
        VBox row = card(label(request.getListingTitle() + " · " + request.getStatus(), "section-title"),
                label("@" + request.getBuyerUsername() + " · " + request.getTypeDisplay() + " · " + time(request.getCreatedAt()), "muted"),
                label(terms.display(request.getType()), "label"));
        if ("Countered".equals(request.getStatus())) {
            row.getChildren().add(label("Counteroffer: " + MarketplaceActivity.counterTerms(request).display(request.getType()), "label"));
        }
        Deal deal = data.findDealForRequest(request.getId());
        if (deal != null && !"Pending".equals(deal.getStatus())) {
            row.getChildren().add(label("Deal: " + deal.getStatus(), "muted"));
        }
        HBox actions = new HBox(8, button("Chat", "btn-outline",
                () -> showChat(activity.chatForRequest(seller, request.getId()))));
        if ("Pending".equals(request.getStatus())) {
            actions.getChildren().addAll(button("Accept", "btn-primary",
                            () -> respond(request, MarketplaceActivity.Decision.ACCEPT, refresh)),
                    button("Decline", "btn-outline", () -> respond(request, MarketplaceActivity.Decision.DECLINE, refresh)));
            if (request.getType() != ListingType.GIVEAWAY) {
                actions.getChildren().add(button("Counter", "btn-outline", () -> termsForm(contentPane, request.getType(),
                        terms, "Send counteroffer", agreement -> {
                            activity.respond(seller, request.getId(), MarketplaceActivity.Decision.COUNTER, agreement);
                            refresh.run();
                        })));
            }
        }
        row.getChildren().add(actions);
        return row;
    }

    private void respond(Request request, MarketplaceActivity.Decision decision, Runnable refresh) {
        String title = decision == MarketplaceActivity.Decision.ACCEPT ? "Accept request?" : "Decline request?";
        if (confirm(contentPane, title, MarketplaceActivity.proposed(request).display(request.getType()))) {
            activity.respond(seller, request.getId(), decision, null);
            refresh.run();
        }
    }

    @FXML
    private void showMessages() { showChat(null); }

    private void showChat(Chat selected) {
        show(btnChat, "Messages", chat(seller, activity, selected, null));
    }

    @FXML
    private void showStats() {
        var stats = marketplace.stats(seller);
        FlowPane metrics = new FlowPane(10, 10,
                metric("Listing views", stats.views()), metric("Offers", stats.offers()),
                metric("Loan requests", stats.loanRequests()), metric("Giveaway claims", stats.claims()),
                metric("Completed deals", stats.completedSales() + stats.returnedLoans() + stats.completedGiveaways()),
                metric("Average rating", stats.ratingCount() == 0 ? "No ratings yet" : String.format("%.1f / 5", stats.averageRating())));
        VBox rows = new VBox(12, metrics, viewsChart(stats), dealChart(stats));
        if (!stats.listings().isEmpty()) {
            rows.getChildren().add(label("Activity by listing", "section-title"));
            for (var item : stats.listings()) {
                rows.getChildren().add(card(label(item.title(), "section-title"),
                        label(item.views() + " views · " + item.offers() + " offers · " + item.loanRequests()
                                + " loan requests · " + item.claims() + " claims · " + item.completedDeals() + " completed", "muted")));
            }
        }
        show(btnStats, "Seller Stats", scroll(rows));
        onLoanUpdate = this::showStats;
    }

    private Node metric(String summary, Object value) {
        VBox metric = card(label(value.toString(), "section-title"), label(summary, "muted"));
        metric.setMinWidth(126);
        return metric;
    }

    private Node viewsChart(SellerMarketplace.Stats stats) {
        if (stats.views() == 0) return card(label("No listing views yet.", "muted"));
        var axis = new CategoryAxis();
        axis.setLabel("Listing");
        var values = new NumberAxis();
        values.setLabel("Views");
        var chart = new BarChart<String, Number>(axis, values);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setTitle("Views by listing");
        chart.setPrefHeight(280);
        var series = new XYChart.Series<String, Number>();
        for (var item : stats.listings()) series.getData().add(new XYChart.Data<>(item.id(), item.views()));
        chart.getData().add(series);
        return card(chart);
    }

    private Node dealChart(SellerMarketplace.Stats stats) {
        if (stats.completedSales() + stats.returnedLoans() + stats.completedGiveaways() == 0) {
            return card(label("No completed deals yet.", "muted"));
        }
        PieChart chart = new PieChart();
        chart.setAnimated(false);
        chart.setTitle("Completed deals by type");
        chart.setPrefHeight(280);
        chart.getData().addAll(new PieChart.Data("Sales (" + stats.completedSales() + ")", stats.completedSales()),
                new PieChart.Data("Loans (" + stats.returnedLoans() + ")", stats.returnedLoans()),
                new PieChart.Data("Giveaways (" + stats.completedGiveaways() + ")", stats.completedGiveaways()));
        return card(chart);
    }

    @FXML
    private void showReviews() { showReviews(null); }

    private void showReviews(String highlight) {
        var reviews = data.getAllRatingsForSeller(seller.getUsername());
        int visible = data.getPublicRatingCount(seller.getUsername());
        String summary = visible == 0 ? "No public ratings yet" : String.format("%.1f / 5 · %d public reviews",
                data.getPublicAverageRating(seller.getUsername()), visible);
        VBox rows = new VBox(10, label(summary, "section-title"));
        if (reviews.isEmpty()) rows.getChildren().add(label("No feedback yet.", "muted"));
        for (Rating rating : reviews) {
            VBox item = card(label(rating.getRating() + " / 5 · " + rating.getListingTitle(), "section-title"),
                    label("@" + rating.getBuyerUsername() + " · " + time(rating.getCreatedAt()), "muted"),
                    label(rating.getComment(), "label"));
            if (rating.getId().equals(highlight)) item.getStyleClass().add("notif-unread");
            if (rating.getSellerReply() != null) item.getChildren().add(label("Your reply: " + rating.getSellerReply(), "label"));
            if (rating.isHidden()) {
                item.getStyleClass().add("hidden-review");
                item.getChildren().add(label("Hidden by moderation", "muted"));
            } else {
                item.getChildren().add(new HBox(8, button(rating.getSellerReply() == null ? "Reply" : "Edit reply", "btn-outline",
                        () -> textForm(contentPane, "Reply to review", "Send reply", "Reply", rating.getSellerReply(), text -> {
                            marketplace.reply(seller, rating.getId(), text);
                            showReviews(rating.getId());
                        })), button("Flag review", "btn-outline",
                        () -> textForm(contentPane, "Flag review", "Send report", "Reason for flagging", "", text -> {
                            marketplace.flag(seller, rating.getId(), text);
                            showReviews(rating.getId());
                        }))));
            }
            rows.getChildren().add(item);
        }
        show(btnReviews, "Reviews & Feedback", scroll(rows));
    }

    @FXML
    private void showNotifications() {
        show(btnNotifications, "Notifications", notifications(seller, data, notice -> {
            String id = notice.getRelatedId();
            switch (notice.getRelatedScreen()) {
                case "CHAT" -> showChat(data.findChat(id));
                case "REQUESTS" -> showRequests(null, id);
                case "DEALS" -> {
                    Deal deal = data.findDeal(id);
                    if (deal == null) showRequests();
                    else showRequests(deal.getListingId(), deal.getRequestId());
                }
                case "LISTINGS" -> showMyListings(id);
                case "RATINGS" -> showReviews(id);
                default -> showNotifications();
            }
        }, this::badge));
    }

    @FXML
    private void showProfile() { show(btnProfile, "My Profile", profile(seller, accounts, this::welcome)); }

    @FXML
    private void logout() {
        SessionManager.logout();
        SceneManager.showWelcome();
    }
}
