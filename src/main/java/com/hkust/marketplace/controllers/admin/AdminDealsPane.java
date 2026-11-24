package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Deal;
import com.hkust.marketplace.models.ListingType;
import com.hkust.marketplace.utils.DummyDataService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Task 3.5 – All Deals Record.
 *
 * <p>Read-only oversight of every deal in the system.  Supports keyword search, deal
 * type, status and start-date range filters.  An invalid date range (start after end)
 * is rejected with an inline error and the previous result set is kept.</p>
 */
final class AdminDealsPane extends VBox {

    private final DummyDataService data = DummyDataService.getInstance();

    private final TextField searchField = new TextField();
    private final ComboBox<String> typeFilter = new ComboBox<>(
            FXCollections.observableArrayList("All Types", "Sell", "Lend", "Giveaway"));
    private final ComboBox<String> statusFilter = new ComboBox<>(
            FXCollections.observableArrayList(
                    "All Statuses", "Pending", "Accepted", "Completed", "Returned", "Auto-returned", "Cancelled"));
    private final DatePicker fromDate = new DatePicker();
    private final DatePicker toDate = new DatePicker();
    private final Label summary = AdminUi.muted("");
    private final Label errorLabel = AdminUi.errorLabel();
    private final TableView<Deal> table = new TableView<>();

    AdminDealsPane(AdminDashboardController host) {
        super(10);
        setPadding(new Insets(4));

        searchField.setPromptText("Search item, buyer or seller...");
        searchField.setPrefWidth(210);
        typeFilter.getSelectionModel().selectFirst();
        statusFilter.getSelectionModel().selectFirst();
        fromDate.setPromptText("From");
        fromDate.setPrefWidth(125);
        toDate.setPromptText("To");
        toDate.setPrefWidth(125);

        Button clear = new Button("Clear");
        clear.getStyleClass().addAll("btn-outline", "btn-sm");
        clear.setOnAction(event -> {
            searchField.clear();
            typeFilter.getSelectionModel().selectFirst();
            statusFilter.getSelectionModel().selectFirst();
            fromDate.setValue(null);
            toDate.setValue(null);
            refresh();
        });

        HBox toolbar = AdminUi.toolbar(searchField, typeFilter, statusFilter,
                fromDate, toDate, clear);
        getChildren().addAll(toolbar, summary, errorLabel);

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().add(table);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());
        typeFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        fromDate.valueProperty().addListener((observable, oldValue, newValue) -> refresh());
        toDate.valueProperty().addListener((observable, oldValue, newValue) -> refresh());

        refresh();
    }

    private void buildTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(AdminUi.emptyState("No deals found",
                "Completed sales, loans and giveaways are recorded here."));

        TableColumn<Deal, String> item = column("Item", Deal::getListingTitle, 160);
        TableColumn<Deal, String> buyer = column("Buyer", Deal::getBuyerUsername, 90);
        TableColumn<Deal, String> seller = column("Seller", Deal::getSellerUsername, 90);
        TableColumn<Deal, String> type = column("Type", Deal::getTypeDisplay, 90);
        TableColumn<Deal, String> start = column("Start Date",
                deal -> AdminUi.formatDate(startDate(deal)), 105);
        TableColumn<Deal, String> end = column("Return / Complete",
                deal -> AdminUi.formatDate(endDate(deal)), 115);
        TableColumn<Deal, String> offer = column("Offer / Period", this::offerDisplay, 140);

        TableColumn<Deal, String> status = new TableColumn<>("Status");
        status.setPrefWidth(100);
        status.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getStatus()));
        status.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                setGraphic(empty || value == null ? null : AdminUi.dealStatusBadge(value));
            }
        });

        table.getColumns().addAll(item, buyer, seller, type, start, end, offer, status);
    }

    private static <T> TableColumn<Deal, T> column(
            String title, Function<Deal, T> value, double width) {
        TableColumn<Deal, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
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
        String wantedType = typeFilter.getValue();
        String wantedStatus = statusFilter.getValue();

        List<Deal> filtered = new ArrayList<>(data.getAllDeals()).stream()
                .filter(deal -> matchesQuery(deal, query))
                .filter(deal -> wantedType == null || "All Types".equals(wantedType)
                        || deal.getTypeDisplay().equals(wantedType))
                .filter(deal -> wantedStatus == null || "All Statuses".equals(wantedStatus)
                        || wantedStatus.equalsIgnoreCase(deal.getStatus()))
                .filter(deal -> {
                    LocalDate start = startDate(deal);
                    if (start == null) return from == null && to == null;
                    if (from != null && start.isBefore(from)) return false;
                    if (to != null && start.isAfter(to)) return false;
                    return true;
                })
                .sorted(Comparator.comparing(Deal::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        table.setItems(FXCollections.observableArrayList(filtered));
        summary.setText(filtered.size() + " deal record(s) "
                + (from == null && to == null ? "" : "in the selected date range"));
    }

    private boolean matchesQuery(Deal deal, String query) {
        if (query.isEmpty()) return true;
        return contains(deal.getId(), query)
                || contains(deal.getListingTitle(), query)
                || contains(deal.getBuyerUsername(), query)
                || contains(deal.getSellerUsername(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private static LocalDate startDate(Deal deal) {
        if (deal.getBorrowStart() != null) return deal.getBorrowStart();
        return deal.getCreatedAt() == null ? null : deal.getCreatedAt().toLocalDate();
    }

    private static LocalDate endDate(Deal deal) {
        if (deal.getCompletedAt() != null) return deal.getCompletedAt().toLocalDate();
        if (deal.getBorrowEnd() != null) return deal.getBorrowEnd();
        return null;
    }

    private String offerDisplay(Deal deal) {
        if (deal.getOfferPrice() != null) {
            return String.format("HK$ %.0f", deal.getOfferPrice());
        }
        if (deal.getBorrowStart() != null && deal.getBorrowEnd() != null) {
            java.time.format.DateTimeFormatter shortDate =
                    java.time.format.DateTimeFormatter.ofPattern("dd MMM");
            return shortDate.format(deal.getBorrowStart()) + " – "
                    + shortDate.format(deal.getBorrowEnd());
        }
        if (deal.getDealType() == ListingType.GIVEAWAY) {
            return "Claim";
        }
        return "—";
    }
}
