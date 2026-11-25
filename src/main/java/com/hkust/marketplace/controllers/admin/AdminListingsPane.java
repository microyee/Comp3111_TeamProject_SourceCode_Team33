package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.controllers.PhotoViewer;
import com.hkust.marketplace.models.Listing;
import com.hkust.marketplace.models.ListingStatus;
import com.hkust.marketplace.models.Notification;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.ImageSupport;
import com.hkust.marketplace.utils.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Task 3.7 – Manage All Listings.
 *
 * <p>Shows every listing regardless of status with keyword/status/type filters.  The
 * admin can edit any listing, add a listing on behalf of an active seller, and take a
 * live listing down with a required reason.  A take-down removes the listing from the
 * marketplace, notifies the seller and every buyer with an active offer, and cancels
 * those offers.</p>
 */
final class AdminListingsPane extends VBox {

    private final AdminDashboardController host;
    private final DummyDataService data = DummyDataService.getInstance();

    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private final Label summary = AdminUi.muted("");
    private final TableView<Listing> table = new TableView<>();

    AdminListingsPane(AdminDashboardController host, String highlightId) {
        super(12);
        this.host = host;
        setPadding(new Insets(4));

        searchField.setPromptText("Search title, seller, category or listing ID...");
        searchField.setPrefWidth(280);

        statusFilter.getItems().add("All Statuses");
        for (ListingStatus status : ListingStatus.values()) {
            statusFilter.getItems().add(AdminUi.statusText(status));
        }
        statusFilter.getSelectionModel().selectFirst();

        typeFilter.getItems().addAll("All Types", "For Sale", "For Lending", "Giveaway");
        typeFilter.getSelectionModel().selectFirst();

        Button addButton = new Button("+ Add Listing");
        addButton.getStyleClass().addAll("btn-primary", "btn-sm");
        addButton.setOnAction(event -> addListing());

        HBox toolbar = AdminUi.toolbar(searchField, statusFilter, typeFilter,
                AdminUi.spacer(), addButton);
        getChildren().addAll(toolbar, summary);

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        typeFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());

        refresh();
        if (highlightId != null) {
            focusListing(highlightId);
        }
    }

    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(AdminUi.emptyState("No listings match",
                "Clear the filters or add a listing on behalf of a seller."));
        table.setRowFactory(tableView -> {
            TableRow<Listing> row = new TableRow<>();
            row.getStyleClass().add("clickable-row");
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1
                        && !row.isEmpty() && !PhotoViewer.isInteractiveTarget(event.getTarget())) {
                    Listing clicked = row.getItem();
                    Platform.runLater(() -> editListing(clicked));
                }
            });
            return row;
        });

        TableColumn<Listing, String> photo = new TableColumn<>("Photo");
        photo.setPrefWidth(62);
        photo.setMinWidth(62);
        photo.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().getPhotoPaths().isEmpty() ? "" : cell.getValue().getPhotoPaths().get(0)));
        photo.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                setText(null);
                if (empty || path == null) {
                    setGraphic(null);
                    return;
                }
                Node thumb = ImageSupport.thumbnail(path, 52, 40);
                Listing listing = getTableView().getItems().get(getIndex());
                int index = Math.max(0, listing.getPhotoPaths().indexOf(path));
                thumb.setOnMouseClicked(event -> {
                    PhotoViewer.show(thumb, listing.getPhotoPaths(), index, listing.getTitle());
                    event.consume();
                });
                setGraphic(thumb);
            }
        });

        TableColumn<Listing, String> title = column("Title", Listing::getTitle, 112);
        TableColumn<Listing, String> seller = column("Seller",
                listing -> listing.getSellerFullName() + " (@" + listing.getSellerUsername() + ")", 98);
        TableColumn<Listing, String> type = column("Type", Listing::getTypeDisplay, 78);
        TableColumn<Listing, String> category = column("Category", Listing::getCategory, 82);
        TableColumn<Listing, String> price = column("Price", Listing::getPriceDisplay, 78);
        TableColumn<Listing, ListingStatus> status = new TableColumn<>("Status");
        status.setPrefWidth(104);
        status.setMinWidth(104);
        status.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getStatus()));
        status.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(ListingStatus value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                setGraphic(empty || value == null ? null
                        : AdminUi.badge(AdminUi.statusShortText(value), AdminUi.statusStyle(value)));
            }
        });

        TableColumn<Listing, String> posted = column("Posted",
                listing -> AdminUi.formatDate(listing.getPostedAt() == null
                        ? null : listing.getPostedAt().toLocalDate()), 76);

        TableColumn<Listing, Void> actions = new TableColumn<>("Actions");
        actions.setPrefWidth(190);
        actions.setMinWidth(190);
        actions.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Void value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                Listing listing = getTableView().getItems().get(getIndex());
                Button edit = new Button("Edit");
                edit.getStyleClass().addAll("btn-outline", "btn-sm");
                edit.setOnAction(event -> editListing(listing));

                Button takeDown = new Button("Take Down");
                takeDown.getStyleClass().addAll("btn-danger", "btn-sm");
                takeDown.setDisable(listing.getStatus() != ListingStatus.LIVE);
                takeDown.setTooltip(new Tooltip("Only live listings can be taken down"));
                takeDown.setOnAction(event -> takeDownListing(listing));

                HBox box = new HBox(6, edit, takeDown);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        table.getColumns().addAll(photo, title, seller, type, category, price, status, posted, actions);
    }

    private static <T> TableColumn<Listing, T> column(
            String title, Function<Listing, T> value, double width) {
        TableColumn<Listing, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void refresh() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String wantedStatus = statusFilter.getValue();
        String wantedType = typeFilter.getValue();

        List<Listing> filtered = new ArrayList<>(data.getAllListings()).stream()
                .filter(listing -> matchesQuery(listing, query))
                .filter(listing -> wantedStatus == null || "All Statuses".equals(wantedStatus)
                        || AdminUi.statusText(listing.getStatus()).equals(wantedStatus))
                .filter(listing -> wantedType == null || "All Types".equals(wantedType)
                        || listing.getTypeDisplay().equals(wantedType))
                .sorted(Comparator.comparing(Listing::getPostedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        table.setItems(FXCollections.observableArrayList(filtered));
        summary.setText(filtered.size() + " listing(s) shown");
    }

    private boolean matchesQuery(Listing listing, String query) {
        if (query.isEmpty()) return true;
        return contains(listing.getId(), query)
                || contains(listing.getTitle(), query)
                || contains(listing.getSellerUsername(), query)
                || contains(listing.getSellerFullName(), query)
                || contains(listing.getCategory(), query)
                || contains(listing.getDescription(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void focusListing(String listingId) {
        searchField.setText(listingId);
        refresh();
        for (int i = 0; i < table.getItems().size(); i++) {
            if (listingId.equalsIgnoreCase(table.getItems().get(i).getId())) {
                table.getSelectionModel().select(i);
                table.scrollTo(i);
                break;
            }
        }
    }

    private void addListing() {
        List<User> sellers = data.getActiveSellers();
        if (sellers.isEmpty()) {
            AdminDialogs.warn("No active sellers",
                    "Create or reactivate a seller account before adding a listing on their behalf.");
            return;
        }

        AdminListingFormDialog dialog = new AdminListingFormDialog(
                AdminUi.window(this), null, sellers);
        dialog.showAndWait().ifPresent(listing -> {
            data.addListing(listing);
            data.addNotification(new Notification(listing.getSellerUsername(),
                    "Listing Created by Admin",
                    "An administrator created '" + listing.getTitle() + "' on your behalf. "
                            + "It is pending review.",
                    "System", "LISTINGS", listing.getId()));
            data.addNotification(new Notification(adminUsername(), "Listing Created",
                    "You created '" + listing.getTitle() + "' for @" + listing.getSellerUsername() + ".",
                    "System", "LISTINGS", listing.getId()));
            data.logAudit(adminUsername(), "CREATE_LISTING", "LISTING", listing.getId(),
                    listing.getTitle() + " for @" + listing.getSellerUsername());
            host.refreshUnreadBadge();
            refresh();
            AdminDialogs.info("Listing created",
                    "Status: Pending Review. It will appear in Listing Review for approval.");
        });
    }

    private void editListing(Listing listing) {
        Listing current = data.findListing(listing.getId());
        if (current == null) {
            AdminDialogs.warn("Listing no longer available",
                    "This listing was removed. The table has been refreshed.");
            refresh();
            return;
        }

        AdminListingFormDialog dialog = new AdminListingFormDialog(
                AdminUi.window(this), current, null);
        dialog.showAndWait().ifPresent(updated -> {
            if (data.findListing(updated.getId()) == null) {
                AdminDialogs.warn("Listing no longer available",
                        "The listing was removed while you were editing it. The table has been refreshed.");
                refresh();
                return;
            }
            data.updateListing(updated);
            data.logAudit(adminUsername(), "EDIT_LISTING", "LISTING", updated.getId(),
                    updated.getTitle());
            refresh();
            AdminDialogs.info("Listing updated",
                    "The listing details were saved and its status was kept as "
                            + AdminUi.statusText(updated.getStatus()) + ".");
        });
    }

    private void takeDownListing(Listing listing) {
        Listing current = data.findListing(listing.getId());
        if (current == null || current.getStatus() != ListingStatus.LIVE) {
            AdminDialogs.warn("Listing no longer live",
                    "Only a live listing can be taken down. The table has been refreshed.");
            refresh();
            return;
        }

        Optional<String> reason = AdminDialogs.promptReason(
                "Take down '" + current.getTitle() + "'",
                "Taking this listing down removes it from the marketplace and notifies the seller "
                        + "and every buyer with an active offer. Their offers will be cancelled.",
                "Removal reason (required)");
        if (reason.isEmpty()) {
            return;
        }
        if (!AdminDialogs.confirmDestructive("Confirm take down",
                "Remove '" + current.getTitle() + "' from the marketplace?\n\nReason: "
                        + reason.get(),
                "Take Down")) {
            return;
        }

        int cancelled = data.takeDownListing(current.getId(), reason.get(), adminUsername());
        if (cancelled < 0) {
            AdminDialogs.warn("Listing no longer live",
                    "The listing changed while you were confirming. The table has been refreshed.");
            refresh();
            return;
        }

        host.refreshUnreadBadge();
        refresh();
        AdminDialogs.info("Listing removed",
                "The seller was notified and " + cancelled
                        + " active offer(s) were cancelled (buyers notified).");
    }

    private String adminUsername() {
        User user = SessionManager.getCurrentUser();
        return user == null ? DummyDataService.ADMIN_USERNAME : user.getUsername();
    }
}
