package com.hkust.marketplace.controllers.seller;

import com.hkust.marketplace.models.Listing;
import com.hkust.marketplace.models.ListingType;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SellerMarketplace;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;

import static com.hkust.marketplace.controllers.PortalUi.*;

final class SellerListingForm {
    private SellerListingForm() {}

    static Node build(User seller, Listing existing, SellerMarketplace marketplace, Runnable done) {
        var type = choice("Select listing type", List.of(ListingType.values()));
        var category = choice("Select category", DummyDataService.CATEGORIES);
        var condition = choice("Select condition", DummyDataService.CONDITIONS);
        var meetup = choice("Select HKUST meet-up", DummyDataService.HKUST_LOCATIONS);
        var title = field("Item name");
        var price = field("Price in HKD");
        var duration = field("Maximum duration in days");
        TextArea description = new TextArea();
        description.setWrapText(true);
        description.setPrefRowCount(4);

        var photos = new LinkedHashSet<String>();
        if (existing == null) {
            meetup.setValue(seller.getPreferredMeetup());
        } else {
            type.setValue(existing.getType());
            category.setValue(existing.getCategory());
            condition.setValue(existing.getCondition());
            meetup.setValue(existing.getMeetupLocation());
            title.setText(existing.getTitle());
            description.setText(existing.getDescription());
            if (existing.getPrice() != null) price.setText(existing.getPrice().toString());
            if (existing.getLendingDays() != null) duration.setText(existing.getLendingDays().toString());
            photos.addAll(existing.getPhotoPaths());
        }

        VBox saleField = formField("Price in HKD", price), loanField = formField("Maximum loan duration in days", duration);
        saleField.visibleProperty().bind(type.valueProperty().isEqualTo(ListingType.SELL));
        saleField.managedProperty().bind(saleField.visibleProperty());
        loanField.visibleProperty().bind(type.valueProperty().isEqualTo(ListingType.LEND));
        loanField.managedProperty().bind(loanField.visibleProperty());

        Label hint = label("", "muted");
        Label status = label("", "error-text");
        FlowPane photoRows = new FlowPane(10, 10);
        VBox panel = card(formField("Listing type", type), formField("Item name", title), formField("Category", category),
                formField("Condition", condition), formField("Description", description), formField("HKUST meet-up", meetup),
                saleField, loanField, hint, photoRows);
        panel.setMaxWidth(680);
        Runnable renderPhotos = () -> renderPhotos(photos, hint, photoRows);
        panel.getChildren().add(button("Add photos", "btn-outline", () -> {
            FileChooser picker = new FileChooser();
            picker.setTitle("Select listing photos");
            picker.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            List<File> files = picker.showOpenMultipleDialog(panel.getScene().getWindow());
            if (files != null) {
                var chosen = new LinkedHashSet<>(photos);
                for (File file : files) chosen.add(file.getAbsolutePath());
                if (chosen.size() > 6) throw new IllegalArgumentException("A listing can contain up to six photos.");
                for (File file : files) {
                    Image image = new Image(file.toURI().toString(), 96, 72, true, true, false);
                    if (image.isError()) throw new IllegalArgumentException("Could not open photo " + file.getName() + ".");
                }
                photos.clear();
                photos.addAll(chosen);
                renderPhotos.run();
            }
        }));
        panel.getChildren().addAll(status, new HBox(8, button(existing == null ? "Submit for review" : "Save and resubmit",
                "btn-primary", () -> {
                    try {
                        var details = new SellerMarketplace.ListingDetails(type.getValue(), title.getText(), category.getValue(),
                                condition.getValue(), description.getText(), meetup.getValue(),
                                type.getValue() == ListingType.SELL ? number(price.getText()) : null,
                                type.getValue() == ListingType.LEND ? days(duration.getText()) : null,
                                List.copyOf(photos));
                        if (existing != null && !confirm(panel, "Resubmit listing?",
                                "The edited listing will await administrator approval before it is visible again.")) return;
                        marketplace.save(seller, existing == null ? null : existing.getId(), details);
                        done.run();
                    } catch (IllegalArgumentException e) {
                        status.setText(e.getMessage());
                    }
                }), button("Cancel", "btn-outline", done)));
        renderPhotos.run();
        return scroll(new VBox(panel));
    }

    private static Integer days(String text) {
        try {
            return Integer.valueOf(text.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter the loan duration in whole days.");
        }
    }

    private static void renderPhotos(LinkedHashSet<String> photos, Label hint, FlowPane rows) {
        hint.setText("Photos (" + photos.size() + " / 6)");
        rows.getChildren().clear();
        for (String path : photos) {
            File file = new File(path);
            Image image = new Image(file.toURI().toString(), 96, 72, true, true, false);
            Node thumbnail = image.isError() ? label("Photo unavailable", "muted") : new ImageView(image);
            rows.getChildren().add(new VBox(4, thumbnail, label(file.getName(), "muted"),
                    button("Remove photo", "btn-outline", () -> {
                        photos.remove(path);
                        renderPhotos(photos, hint, rows);
                    })));
        }
    }
}
