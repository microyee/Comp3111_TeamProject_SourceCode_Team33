package com.hkust.marketplace.controllers.buyer;

import com.hkust.marketplace.MarketplaceApp;
import com.hkust.marketplace.models.*;
import com.hkust.marketplace.utils.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static com.hkust.marketplace.controllers.PortalUi.*;

public class BuyerDashboardController {
    @FXML private StackPane contentPane;
    @FXML private Label pageTitle, welcomeLabel, unreadLabel;
    @FXML private Button btnBrowse, btnDeals, btnHistory, btnFavourites, btnFollowing, btnChat, btnNotifications, btnProfile;

    private final DummyDataService data = DummyDataService.getInstance();
    private final MarketplaceAccounts accounts = new MarketplaceAccounts(data);
    private final MarketplaceCatalog catalog = data.catalog();
    private final MarketplaceActivity activity = data.activity();
    private Button active;
    private User buyer;
    private Runnable onLoanUpdate = () -> {};

    @FXML
    private void initialize() {
        if (!SessionManager.isSessionValid() || !SessionManager.isBuyer()) {
            SessionManager.logout();
            Platform.runLater(SceneManager::showBuyerLogin);
            return;
        }
        buyer = SessionManager.getCurrentUser();
        welcome();
        contentPane.addEventHandler(MarketplaceApp.LOANS_UPDATED, event -> {
            badge();
            onLoanUpdate.run();
        });
        showBrowse();
    }

    private void welcome() {
        welcomeLabel.setText("Buyer: " + buyer.getFullName());
    }

    private void badge() {
        int count = data.getUnreadCount(buyer.getUsername());
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
    private void showBrowse() {
        TextField search = field("Search listings");
        HBox.setHgrow(search, Priority.ALWAYS);
        var type = choice("All types", List.of(ListingType.values()));
        var category = choice("All categories", DummyDataService.CATEGORIES);
        var condition = choice("All conditions", DummyDataService.CONDITIONS);
        var sort = choice("Sort", List.of(MarketplaceCatalog.Sort.values()));
        sort.setValue(MarketplaceCatalog.Sort.NEWEST);
        TextField min = field("Min HKD");
        TextField max = field("Max HKD");
        min.setPrefColumnCount(6);
        max.setPrefColumnCount(6);
        FlowPane filters = new FlowPane(8, 8, type, category, condition, min, max, sort);
        VBox rows = new VBox(10);
        Label validation = label("", "error-text");
        validation.visibleProperty().bind(validation.textProperty().isNotEmpty());
        validation.managedProperty().bind(validation.visibleProperty());
        Runnable render = () -> {
            try {
                var filter = new MarketplaceCatalog.Filter(search.getText(), type.getValue(), category.getValue(),
                        condition.getValue(), number(min.getText()), number(max.getText()), sort.getValue());
                List<Listing> matches = catalog.browse(buyer, filter);
                validation.setText("");
                rows.getChildren().setAll(matches.stream().map(listing -> listingRow(listing,
                        this::restoreBrowse)).toList());
                if (matches.isEmpty()) rows.getChildren().add(label("No listings match these filters.", "muted"));
            } catch (IllegalArgumentException e) {
                validation.setText(e.getMessage());
            }
        };
        search.textProperty().addListener((obs, old, value) -> render.run());
        for (ComboBox<?> combo : List.of(type, category, condition, sort)) combo.setOnAction(event -> render.run());
        min.textProperty().addListener((obs, old, value) -> render.run());
        max.textProperty().addListener((obs, old, value) -> render.run());
        Button clear = button("Clear filters", "btn-outline", () -> {
            search.clear();
            type.setValue(null);
            category.setValue(null);
            condition.setValue(null);
            min.clear();
            max.clear();
            sort.setValue(MarketplaceCatalog.Sort.NEWEST);
        });
        VBox toolbar = new VBox(8, new HBox(8, search, clear), filters, validation);
        toolbar.getStyleClass().add("toolbar");
        ScrollPane scroll = scroll(rows);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        browsePage = new VBox(12, toolbar, scroll);
        browseRender = render;
        restoreBrowse();
    }

    private void restoreBrowse() {
        show(btnBrowse, "Browse Marketplace", browsePage);
        onLoanUpdate = browseRender;
        browseRender.run();
    }

    private Node browsePage;
    private Runnable browseRender;

    private Node listingRow(Listing listing, Runnable back) {
        VBox summary = new VBox(5, label(listing.getTitle(), "section-title"),
                label(listing.getTypeDisplay() + " · " + listing.getPriceDisplay() + " · " + listing.getCondition()
                        + " · " + listing.getCategory(), "label"),
                label("@" + listing.getSellerUsername() + " · " + listing.getMeetupLocation()
                        + " · Posted " + time(listing.getPostedAt()) + " · " + listing.getStatus(), "muted"));
        HBox.setHgrow(summary, Priority.ALWAYS);
        HBox row = new HBox(14, thumbnail(listing, 86, 68), summary,
                button("View listing", "btn-primary", () -> openListing(listing.getId(), back)));
        row.setAlignment(Pos.CENTER_LEFT);
        return card(row);
    }

    private void openListing(String id, Runnable back) {
        showListing(catalog.openListing(buyer, id), back);
    }

    private void showListing(Listing listing, Runnable back) {
        String id = listing.getId();
        VBox details = card(label(listing.getTitle(), "section-title"),
                label(listing.getTypeDisplay() + " · " + listing.getPriceDisplay() + " · " + listing.getStatus(), "label"),
                label(listing.getCategory() + " · " + listing.getCondition(), "muted"),
                label(listing.getDescription(), "label"), label("Meet-up: " + listing.getMeetupLocation(), "label"),
                label("Posted " + time(listing.getPostedAt()), "muted"));
        if (listing.getLendingDays() != null) details.getChildren().add(label("Maximum loan: " + listing.getLendingDays() + " days", "label"));
        FlowPane photos = new FlowPane(10, 10);
        for (String path : listing.getPhotoPaths()) photos.getChildren().add(photo(path, 220, 155));
        if (photos.getChildren().isEmpty()) photos.getChildren().add(thumbnail(listing, 180, 112));
        details.getChildren().add(photos);

        String seller = listing.getSellerUsername();
        var actions = new FlowPane(8, 8);
        actions.getChildren().add(button("Seller profile", "btn-outline", () -> showSeller(seller, () -> showListing(listing, back))));
        boolean publicListing = catalog.isPublic(listing);
        boolean saved = catalog.isFavourite(buyer, id);
        if (publicListing || saved) {
            actions.getChildren().add(button(saved ? "Remove favourite" : "Save favourite", "btn-outline", () -> {
                catalog.favourite(buyer, id, !saved);
                showListing(listing, back);
            }));
        }
        if (publicListing) {
            actions.getChildren().addAll(button(requestAction(listing.getType()), "btn-primary",
                            () -> request(listing, this::showMyDeals)),
                    button("Report listing", "btn-outline", () -> textForm(contentPane, "Report listing", "Send report",
                            "Reason for reporting", "", text -> catalog.reportListing(buyer, id, text))));
        }
        actions.getChildren().add(button("Chat", "btn-outline", () -> showChat(activity.openChat(buyer, id))));
        details.getChildren().add(actions);
        String rating = ratingSummary(seller);
        details.getChildren().add(label(listing.getSellerFullName() + " · " + rating, "muted"));
        show(active, "Listing Details", scroll(new VBox(12, button("Back", "btn-outline", back), details)));
    }

    private static String requestAction(ListingType type) {
        return switch (type) {
            case SELL -> "Make offer / bid";
            case LEND -> "Request to borrow";
            case GIVEAWAY -> "Claim giveaway";
        };
    }

    private void request(Listing listing, Runnable done) {
        if (listing == null || !catalog.isPublic(listing)) throw new IllegalArgumentException("This listing is unavailable.");
        termsForm(contentPane, listing.getType(), new MarketplaceActivity.Terms(listing.getPrice(), null, null),
                requestAction(listing.getType()), terms -> {
                    activity.submit(buyer, listing.getId(), terms);
                    done.run();
                });
    }

    @FXML
    private void showFavourites() {
        VBox rows = new VBox(10);
        for (Listing listing : catalog.favourites(buyer)) {
            if (catalog.isPublic(listing)) rows.getChildren().add(listingRow(listing, this::showFavourites));
            else rows.getChildren().add(card(label(listing.getTitle(), "section-title"),
                    label("Currently unavailable", "muted"), button("Remove favourite", "btn-outline", () -> {
                        catalog.favourite(buyer, listing.getId(), false);
                        showFavourites();
                    })));
        }
        if (rows.getChildren().isEmpty()) rows.getChildren().add(label("No saved listings yet.", "muted"));
        show(btnFavourites, "Favourites", scroll(rows));
        onLoanUpdate = this::showFavourites;
    }

    @FXML
    private void showFollowing() {
        VBox rows = new VBox(10);
        for (User seller : catalog.following(buyer)) {
            rows.getChildren().add(card(label(seller.getFullName() + " · @" + seller.getUsername(), "section-title"),
                    new HBox(8, button("View profile", "btn-primary", () -> showSeller(seller.getUsername(), this::showFollowing)),
                            button("Unfollow", "btn-outline", () -> {
                                catalog.follow(buyer, seller.getUsername(), false);
                                showFollowing();
                            }))));
        }
        if (rows.getChildren().isEmpty()) rows.getChildren().add(label("You aren’t following any sellers yet.", "muted"));
        show(btnFollowing, "Following", scroll(rows));
    }

    private void showSeller(String username, Runnable back) {
        User seller = data.findUser(username);
        if (seller == null || !seller.isActive() || seller.isDeleted()) throw new IllegalArgumentException("Seller profile is unavailable.");
        boolean followed = catalog.isFollowing(buyer, username);
        VBox content = new VBox(10, button("Back", "btn-outline", back),
                card(label(seller.getFullName() + " · @" + username, "section-title"),
                        label(seller.getBio(), "label"), label(ratingSummary(username), "muted"),
                        button(followed ? "Unfollow" : "Follow seller", "btn-primary", () -> {
                            catalog.follow(buyer, username, !followed);
                            showSeller(username, back);
                        })));
        for (Rating rating : data.getRatingsForSeller(username)) content.getChildren().add(reviewRow(rating));
        show(active, "Seller Profile", scroll(content));
    }

    private String ratingSummary(String username) {
        int count = data.getPublicRatingCount(username);
        return count == 0 ? "No ratings yet" : String.format("%.1f / 5 · %d reviews", data.getPublicAverageRating(username), count);
    }

    private Node reviewRow(Rating rating) {
        VBox row = card(label(rating.getRating() + " / 5 · " + rating.getListingTitle(), "section-title"),
                label("@" + rating.getBuyerUsername() + " · " + time(rating.getCreatedAt()), "muted"),
                label(rating.getComment(), "label"));
        if (rating.getSellerReply() != null) row.getChildren().add(label("Seller reply: " + rating.getSellerReply(), "label"));
        if (rating.isHidden()) row.getChildren().add(label("Hidden by moderation", "muted"));
        return row;
    }

    @FXML
    private void showMyDeals() {
        VBox rows = new VBox(10);
        data.getRequestsForBuyer(buyer.getUsername()).stream().filter(Request::isActive).forEach(r -> rows.getChildren().add(requestRow(r)));
        data.getDealsForBuyer(buyer.getUsername()).stream().filter(d -> "Accepted".equals(d.getStatus()))
                .forEach(d -> rows.getChildren().add(dealRow(d, this::showMyDeals)));
        if (rows.getChildren().isEmpty()) rows.getChildren().add(label("No open requests or active deals.", "muted"));
        List<Request> closed = data.getRequestsForBuyer(buyer.getUsername()).stream()
                .filter(r -> "Declined".equals(r.getStatus()) || "Cancelled".equals(r.getStatus())).toList();
        if (!closed.isEmpty()) {
            rows.getChildren().add(label("Closed requests", "section-title"));
            closed.forEach(r -> rows.getChildren().add(requestRow(r)));
        }
        show(btnDeals, "My Deals", scroll(rows));
        onLoanUpdate = this::showMyDeals;
    }

    private Node requestRow(Request request) {
        HBox actions = new HBox(8, button("Listing", "btn-outline", () -> openListing(request.getListingId(), this::showMyDeals)),
                button("Chat", "btn-outline", () -> showChat(activity.chatForRequest(buyer, request.getId()))));
        VBox row = card(label(request.getListingTitle() + " · " + request.getStatus(), "section-title"),
                label("@" + request.getSellerUsername() + " · " + MarketplaceActivity.proposed(request).display(request.getType()), "label"));
        if ("Countered".equals(request.getStatus())) {
            row.getChildren().add(label("Counteroffer: " + MarketplaceActivity.counterTerms(request).display(request.getType()), "label"));
            actions.getChildren().addAll(button("Accept counteroffer", "btn-primary", () -> respond(request, MarketplaceActivity.Decision.ACCEPT)),
                    button("Decline counteroffer", "btn-outline", () -> respond(request, MarketplaceActivity.Decision.DECLINE)));
        }
        if (request.isActive()) actions.getChildren().add(button("Withdraw", "btn-outline", () -> {
            if (confirm(contentPane, "Withdraw request?", request.getListingTitle())) {
                activity.withdraw(buyer, request.getId());
                showMyDeals();
            }
        }));
        row.getChildren().add(actions);
        return row;
    }

    private void respond(Request request, MarketplaceActivity.Decision decision) {
        if (confirm(contentPane, decision == MarketplaceActivity.Decision.ACCEPT ? "Accept counteroffer?" : "Decline counteroffer?",
                MarketplaceActivity.counterTerms(request).display(request.getType()))) {
            activity.respond(buyer, request.getId(), decision, null);
            showMyDeals();
        }
    }

    private Node dealRow(Deal deal, Runnable back) {
        VBox row = card(label(deal.getListingTitle() + " · " + deal.getStatus(), "section-title"),
                label(deal.getTypeDisplay() + " · @" + deal.getSellerUsername() + " · " + time(deal.getCreatedAt()), "muted"));
        if (deal.getBorrowEndAt() != null) row.getChildren().add(label("Loan: " + time(deal.getBorrowStartAt())
                + " to " + time(deal.getBorrowEndAt()), "label"));
        if (deal.getCompletedAt() != null) row.getChildren().add(label("Completed " + time(deal.getCompletedAt()), "muted"));
        if (deal.getOfferPrice() != null) row.getChildren().add(label("Agreed price: HK$ " + deal.getOfferPrice(), "label"));
        HBox actions = new HBox(8, button("Listing", "btn-outline", () -> openListing(deal.getListingId(), back)),
                button("Chat", "btn-outline", () -> showChat(activity.openChat(buyer, deal.getListingId()))));
        if ("Accepted".equals(deal.getStatus())) {
            boolean loan = deal.getDealType() == ListingType.LEND;
            if (!loan || deal.isLoanStarted()) {
                String action = loan ? "Return item" : "Complete deal";
                actions.getChildren().add(button(action, "btn-primary", () -> {
                    if (confirm(contentPane, action + "?", deal.getListingTitle())) {
                        activity.finish(buyer, deal.getId());
                        showHistory();
                        if (review(deal) == null && !deal.isRated()) rate(deal);
                    }
                }));
            }
        } else if (deal.isFinished()) {
            Rating review = review(deal);
            if (review != null) row.getChildren().add(reviewRow(review));
            else if (!deal.isRated()) actions.getChildren().add(button("Rate seller", "btn-primary", () -> rate(deal)));
        }
        row.getChildren().add(actions);
        return row;
    }

    private Rating review(Deal deal) {
        return data.getAllRatings().stream().filter(r -> deal.getId().equals(r.getDealId())).findFirst().orElse(null);
    }

    private void rate(Deal deal) {
        var stars = choice("Stars", List.of(1, 2, 3, 4, 5));
        TextArea comment = new TextArea();
        comment.setPrefRowCount(3);
        comment.setWrapText(true);
        form(contentPane, "Rate seller", "Publish review", new VBox(12, formField("Stars", stars),
                formField("Comments (optional)", comment)), () -> {
                    activity.rate(buyer, deal.getId(), stars.getValue() == null ? 0 : stars.getValue(), comment.getText());
                    showHistory();
                });
    }

    @FXML
    private void showHistory() {
        TextField query = field("Search by listing or seller");
        var category = choice("All categories", DummyDataService.CATEGORIES);
        DatePicker from = new DatePicker(), to = new DatePicker();
        from.setEditable(false);
        to.setEditable(false);
        from.setPromptText("From");
        to.setPromptText("To");
        from.setAccessibleText("From completion date");
        to.setAccessibleText("To completion date");
        VBox rows = new VBox(10);
        Label error = label("", "error-text");
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        error.managedProperty().bind(error.visibleProperty());
        Runnable render = () -> {
            LocalDate first = from.getValue(), last = to.getValue();
            if (first != null && last != null && first.isAfter(last)) {
                error.setText("The end date must be on or after the start date.");
                return;
            }
            error.setText("");
            rows.getChildren().clear();
            String text = query.getText().strip().toLowerCase(Locale.ROOT);
            for (Deal deal : data.getDealsForBuyer(buyer.getUsername())) {
                if (!deal.isFinished() || !matchesHistory(deal, text, category.getValue(), first, last)) continue;
                rows.getChildren().add(dealRow(deal, this::showHistory));
            }
            if (rows.getChildren().isEmpty()) rows.getChildren().add(label("No matching completed deals.", "muted"));
        };
        query.textProperty().addListener((obs, old, value) -> render.run());
        category.setOnAction(event -> render.run());
        from.setOnAction(event -> render.run());
        to.setOnAction(event -> render.run());
        Button reset = button("Clear filters", "btn-outline", () -> {
            query.clear();
            category.setValue(null);
            from.setValue(null);
            to.setValue(null);
        });
        FlowPane toolbar = new FlowPane(8, 8, query, category, from, to, reset);
        toolbar.getStyleClass().add("toolbar");
        ScrollPane scroll = scroll(rows);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        show(btnHistory, "Deal History", new VBox(12, toolbar, error, scroll));
        onLoanUpdate = render;
        render.run();
    }

    private boolean matchesHistory(Deal deal, String query, String category, LocalDate from, LocalDate to) {
        User seller = data.findUser(deal.getSellerUsername());
        if (!MarketplaceCatalog.contains(deal.getListingTitle(), query)
                && !MarketplaceCatalog.contains(deal.getSellerUsername(), query)
                && (seller == null || !MarketplaceCatalog.contains(seller.getFullName(), query))) return false;
        if (category != null && !category.equals(deal.getCategory())) return false;
        LocalDate date = deal.getCompletedAt() == null ? deal.getCreatedAt().toLocalDate() : deal.getCompletedAt().toLocalDate();
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    @FXML
    private void showMessages() { showChat(null); }

    private void showChat(Chat chat) {
        show(btnChat, "Messages", chat(buyer, activity, chat,
                thread -> request(data.findListing(thread.getListingId()), () -> showChat(thread))));
    }

    @FXML
    private void showNotifications() {
        show(btnNotifications, "Notifications", notifications(buyer, data, notice -> {
            switch (notice.getRelatedScreen()) {
                case "CHAT" -> showChat(data.findChat(notice.getRelatedId()));
                case "REQUESTS" -> {
                    Request request = data.findRequest(notice.getRelatedId());
                    if (request != null && request.isActive()) showMyDeals();
                    else if (request != null) showChat(activity.chatForRequest(buyer, request.getId()));
                }
                case "DEALS" -> {
                    Deal deal = data.findDeal(notice.getRelatedId());
                    if (deal == null || !buyer.getUsername().equals(deal.getBuyerUsername())) showNotifications();
                    else if (deal.isFinished()) showHistory();
                    else if ("Accepted".equals(deal.getStatus())) showMyDeals();
                    else if (deal.getRequestId() != null) showChat(activity.chatForRequest(buyer, deal.getRequestId()));
                }
                case "LISTING" -> openListing(notice.getRelatedId(), this::showNotifications);
                case "RATINGS" -> showHistory();
                default -> showNotifications();
            }
        }, this::badge));
    }

    @FXML
    private void showProfile() { show(btnProfile, "My Profile", profile(buyer, accounts, this::welcome)); }

    @FXML
    private void logout() {
        SessionManager.logout();
        SceneManager.showWelcome();
    }

    private static Node thumbnail(Listing listing, int width, int height) {
        if (!listing.getPhotoPaths().isEmpty()) return photo(listing.getPhotoPaths().get(0), width, height);
        return photo("", width, height);
    }

    private static Node photo(String url, int width, int height) {
        try {
            if (!url.isBlank()) {
                String source = new File(url).toURI().toString();
                Image image = new Image(source, width, height, true, true, false);
                if (!image.isError()) {
                    ImageView view = new ImageView(image);
                    view.getStyleClass().add("listing-photo");
                    view.setAccessibleText("Listing photo");
                    return view;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // A missing photo leaves the listing and its actions available.
        }
        StackPane placeholder = new StackPane(new Label("No photo"));
        placeholder.setMinSize(width, height);
        placeholder.getStyleClass().add("photo-placeholder");
        return placeholder;
    }
}
