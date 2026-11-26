package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Buyer;
import com.hkust.marketplace.models.Rating;
import com.hkust.marketplace.models.Seller;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.testsupport.FxTest;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SessionManager;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminRatingsPaneTest {

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

    private static Rating rating(String id, String seller, String buyer, int stars, boolean hidden,
                                 String title, String comment, String reply, LocalDateTime created) {
        Rating rating = new Rating();
        rating.setId(id);
        rating.setSellerUsername(seller);
        rating.setBuyerUsername(buyer);
        rating.setRating(stars);
        rating.setHidden(hidden);
        rating.setListingTitle(title);
        rating.setComment(comment);
        rating.setSellerReply(reply);
        rating.setCreatedAt(created);
        DummyDataService.getInstance().addRating(rating);
        return rating;
    }

    private static Parent cardWithLabel(Parent root, String text) {
        FxTest.layout(root);
        return root.lookupAll(".card").stream()
                .filter(Parent.class::isInstance)
                .map(Parent.class::cast)
                .filter(card -> FxTest.hasLabel(card, text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No card for " + text));
    }

    @Test
    void filtersRatingsAndModeratesVisibility() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        String seller = "ratings-cov-seller";
        String buyer = "ratings-cov-buyer";
        data.registerUser(new Seller(seller, "Ratings Seller", "Password1"));
        data.registerUser(new Buyer(buyer, "Ratings Buyer", "Password1"));
        SessionManager.login(data.findUser("admin"));

        LocalDateTime now = LocalDateTime.now();
        Rating visible = rating("RCV-1", seller, buyer, 5, false, "RCV listing", "Unique rating comment", "Thanks!", now.minusDays(1));
        Rating hidden = rating("RCV-2", seller, buyer, 3, true, null, "   ", "  ", null);
        Rating noComment = rating("RCV-3", seller, buyer, 1, false, "RCV other", null, null, now);
        rating("RCV-4", seller, buyer, 4, false, "RCV third", "Good", null, now.minusDays(2));

        FxTest.run(() -> {
            AdminRatingsPane pane = new AdminRatingsPane(new AdminDashboardController(), seller);
            Stage stage = FxTest.stage(pane);
            TextField search = FxTest.field(pane, "Search seller, buyer or listing...");
            assertEquals(seller, search.getText());
            assertTrue(pane.lookupAll(".card").size() >= 4);

            ComboBox<String> score = comboWith(pane, "5 stars");
            ComboBox<String> visibility = comboWith(pane, "Visible only");
            DatePicker from = datePicker(pane, "From");
            DatePicker to = datePicker(pane, "To");
            Label error = (Label) pane.lookup(".error-text");
            Button clear = FxTest.button(pane, "Clear");

            search.setText("RCV-1");
            assertEquals(1, pane.lookupAll(".card").size());
            search.setText("RCV listing");
            assertEquals(1, pane.lookupAll(".card").size());
            search.setText("Unique rating comment");
            assertEquals(1, pane.lookupAll(".card").size());
            search.setText("no-such-rating");
            assertTrue(FxTest.hasLabel(pane, "No matching ratings"));
            search.setText(seller);

            score.setValue("5 stars");
            assertEquals(1, pane.lookupAll(".card").size());
            score.setValue("3 stars");
            assertEquals(1, pane.lookupAll(".card").size());
            score.setValue("All ratings");
            assertTrue(pane.lookupAll(".card").size() >= 4);

            visibility.setValue("Visible only");
            assertFalse(pane.lookupAll(".card").isEmpty());
            visibility.setValue("Hidden only");
            assertEquals(1, pane.lookupAll(".card").size());
            visibility.setValue("All reviews");

            from.setValue(LocalDate.now());
            to.setValue(LocalDate.now().minusDays(10));
            assertTrue(error.isVisible());
            to.setValue(LocalDate.now().plusDays(1));
            assertFalse(error.isVisible());
            from.setValue(null);
            to.setValue(null);

            clear.fire();
            assertEquals("", search.getText());

            search.setText(seller);
            Button hide = FxTest.button(cardWithLabel(pane, "Unique rating comment"), "Hide");
            FxTest.withDialogs(hide, dialog -> FxTest.button(dialog, "Cancel").fire());
            assertFalse(visible.isHidden());

            hide = FxTest.button(cardWithLabel(pane, "Unique rating comment"), "Hide");
            FxTest.withDialogs(hide, dialog -> FxTest.button(dialog, "OK").fire());
            assertTrue(data.findRating("RCV-1").isHidden());
            assertTrue(pane.lookupAll(".hidden-review").size() >= 2);

            Button unhide = FxTest.button(cardWithLabel(pane, "Unique rating comment"), "Unhide");
            FxTest.withDialogs(unhide, dialog -> FxTest.button(dialog, "OK").fire());
            assertFalse(data.findRating("RCV-1").isHidden());

            pane.applyCss();
            pane.layout();
            stage.hide();
        });
        SessionManager.logout();
    }

    @Test
    void hidesTheLastVisibleRatingAndShowsTheEmptyState() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        String seller = "ratings-only-seller";
        data.registerUser(new Seller(seller, "Only Seller", "Password1"));
        Rating only = rating("RCV-ONLY", seller, "alice", 2, false, "Only listing",
                "Only comment", null, LocalDateTime.now());
        SessionManager.login(data.findUser("admin"));

        FxTest.run(() -> {
            AdminRatingsPane pane = new AdminRatingsPane(new AdminDashboardController(), "RCV-ONLY");
            Stage stage = FxTest.stage(pane);
            assertEquals(1, pane.lookupAll(".card").size());

            FxTest.withDialogs(FxTest.button(pane, "Hide"), dialog -> FxTest.button(dialog, "OK").fire());
            assertTrue(only.isHidden());

            Button unhide = FxTest.button(pane, "Unhide");
            FxTest.withDialogs(unhide, dialog -> FxTest.button(dialog, "OK").fire());
            assertFalse(only.isHidden());
            stage.hide();
        });
        SessionManager.logout();
    }
}
