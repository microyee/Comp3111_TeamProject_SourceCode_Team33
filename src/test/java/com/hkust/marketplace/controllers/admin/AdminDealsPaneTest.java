package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Deal;
import com.hkust.marketplace.models.ListingType;
import com.hkust.marketplace.testsupport.FxTest;
import com.hkust.marketplace.utils.DummyDataService;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminDealsPaneTest {

    @BeforeAll
    static void startFx() {
        FxTest.ensureFx();
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> comboWith(Parent root, String item) {
        return (ComboBox<String>) root.lookupAll(".combo-box").stream()
                .filter(ComboBox.class::isInstance)
                .map(ComboBox.class::cast)
                .filter(combo -> combo.getItems().contains(item))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No combo containing " + item));
    }

    private static DatePicker datePicker(Parent root, String prompt) {
        return root.lookupAll(".date-picker").stream()
                .filter(DatePicker.class::isInstance)
                .map(DatePicker.class::cast)
                .filter(picker -> prompt.equals(picker.getPromptText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No date picker " + prompt));
    }

    private static TableView<?> table(Parent root) {
        return (TableView<?>) root.lookup(".table-view");
    }

    private static Deal deal(String title, ListingType type, String status, Double price,
                             LocalDateTime created, LocalDate borrowStart, LocalDate borrowEnd,
                             LocalDateTime completed) {
        Deal deal = new Deal();
        deal.setListingTitle(title);
        deal.setListingId("DEAL-" + title);
        deal.setBuyerUsername("alice");
        deal.setSellerUsername("charlie");
        deal.setDealType(type);
        deal.setStatus(status);
        deal.setOfferPrice(price);
        deal.setCreatedAt(created);
        deal.setBorrowStart(borrowStart);
        deal.setBorrowEnd(borrowEnd);
        deal.setCompletedAt(completed);
        deal.setCategory("Electronics");
        DummyDataService.getInstance().addDeal(deal);
        return deal;
    }

    @Test
    void filtersDateRangesAndCellRenderingCoverEveryBranch() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        LocalDateTime now = LocalDateTime.now();
        deal("ADMIN COV SELL", ListingType.SELL, "Pending", 120.0, now.minusDays(2), null, null, null);
        deal("ADMIN COV LEND", ListingType.LEND, "Accepted", null, now.minusDays(10),
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(3), null);
        deal("ADMIN COV GIVEAWAY", ListingType.GIVEAWAY, "Completed", null, now.minusDays(40),
                null, null, now.minusDays(35));
        Deal undated = deal("ADMIN COV UNDATED", null, "Cancelled", null, null, null, null, null);

        FxTest.run(() -> {
            AdminDealsPane pane = new AdminDealsPane(new AdminDashboardController());
            Stage stage = FxTest.stage(pane);
            TableView<?> table = table(pane);
            TextField search = FxTest.field(pane, "Search item, buyer or seller...");
            ComboBox<String> type = comboWith(pane, "All Types");
            ComboBox<String> status = comboWith(pane, "All Statuses");
            DatePicker from = datePicker(pane, "From");
            DatePicker to = datePicker(pane, "To");
            Label error = (Label) pane.lookup(".error-text");
            Button clear = FxTest.button(pane, "Clear");

            int allDeals = data.getAllDeals().size();
            assertEquals(allDeals, table.getItems().size());

            search.setText("ADMIN COV SELL");
            assertEquals(1, table.getItems().size());
            search.setText("no-such-deal");
            assertEquals(0, table.getItems().size());
            search.clear();

            for (String typeName : List.of("Sell", "Lend", "Giveaway")) {
                type.setValue(typeName);
                assertTrue(table.getItems().stream()
                        .allMatch(item -> typeName.equals(((Deal) item).getTypeDisplay())));
            }
            type.setValue("All Types");

            for (String statusName : List.of("Pending", "Accepted", "Completed",
                    "Returned", "Auto-returned", "Cancelled")) {
                status.setValue(statusName);
                assertTrue(table.getItems().stream()
                        .allMatch(item -> statusName.equalsIgnoreCase(((Deal) item).getStatus())));
            }
            status.setValue("All Statuses");

            from.setValue(LocalDate.now().minusDays(1));
            to.setValue(LocalDate.now().minusDays(365));
            assertTrue(error.isVisible());
            assertTrue(error.getText().contains("Invalid date range"));
            int kept = table.getItems().size();

            to.setValue(LocalDate.now().plusDays(1));
            assertFalse(error.isVisible());
            assertTrue(table.getItems().size() <= kept);
            assertTrue(table.getItems().stream().noneMatch(item -> item == undated));

            from.setValue(null);
            assertFalse(table.getItems().isEmpty());
            to.setValue(null);
            assertFalse(table.getItems().isEmpty());

            clear.fire();
            assertEquals(allDeals, table.getItems().size());

            table.refresh();
            pane.applyCss();
            pane.layout();
            stage.hide();
        });
    }
}
