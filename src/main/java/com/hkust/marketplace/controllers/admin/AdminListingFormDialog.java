package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Deal;
import com.hkust.marketplace.models.Listing;
import com.hkust.marketplace.models.ListingStatus;
import com.hkust.marketplace.models.ListingType;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.utils.DummyDataService;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Task 3.7 – Add / edit listing form used by the admin.
 *
 * <p>Mirrors the seller Create New Listing fields (type first, then item name,
 * category, condition, description, price or lending duration, meet-up location and up
 * to six photos).  New admin-created listings start as Pending Review, exactly like
 * seller submissions; edits keep the listing's current status.</p>
 */
final class AdminListingFormDialog extends Dialog<Listing> {

    private final Listing existing;
    private final List<String> photos = new ArrayList<>();

    private final ComboBox<String> typeCombo = new ComboBox<>();
    private final TextField titleField = new TextField();
    private final ComboBox<String> categoryCombo = new ComboBox<>();
    private final ComboBox<String> conditionCombo = new ComboBox<>();
    private final TextArea descriptionArea = new TextArea();
    private final TextField priceField = new TextField();
    private final TextField daysField = new TextField();
    private final ComboBox<String> meetupCombo = new ComboBox<>();
    private final ComboBox<User> sellerCombo = new ComboBox<>();

    private final VBox priceBox;
    private final VBox daysBox;
    private final FlowPane photoPane = new FlowPane(8, 8);
    private final Label photoCountLabel = AdminUi.hint("0 / 6 photos");
    private final Button addPhotoButton = new Button("+ Add Photos");
    private final Label errorLabel = AdminUi.errorLabel();

    AdminListingFormDialog(Window owner, Listing existing, List<User> sellers) {
        this.existing = existing;
        if (owner != null) {
            initOwner(owner);
        }
        boolean addMode = existing == null;
        setTitle(addMode ? "Add Listing" : "Edit Listing");
        setHeaderText(addMode
                ? "Create a listing on behalf of a seller"
                : "Edit listing: " + existing.getTitle());

        ButtonType save = new ButtonType(addMode ? "Create Listing" : "Save Changes",
                ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        // ---- field setup -------------------------------------------------
        typeCombo.getItems().addAll("Sell", "Lend", "Give Away");
        typeCombo.setPromptText("Listing type *");
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        titleField.setPromptText("Item name *");

        categoryCombo.getItems().addAll(DummyDataService.CATEGORIES);
        categoryCombo.setPromptText("Category *");
        categoryCombo.setMaxWidth(Double.MAX_VALUE);

        conditionCombo.getItems().addAll(DummyDataService.CONDITIONS);
        conditionCombo.setPromptText("Condition *");
        conditionCombo.setMaxWidth(Double.MAX_VALUE);

        descriptionArea.setPromptText("Description *");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);

        priceField.setPromptText("Price in HK$ (required for Sell)");
        daysField.setPromptText("Lending duration in days (required for Lend, 1–365)");

        meetupCombo.getItems().addAll(DummyDataService.HKUST_LOCATIONS);
        meetupCombo.setPromptText("Meet-up location *");
        meetupCombo.setMaxWidth(Double.MAX_VALUE);

        sellerCombo.setPromptText("Choose a seller *");
        sellerCombo.setMaxWidth(Double.MAX_VALUE);
        sellerCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getFullName() + " (@" + user.getUsername() + ")";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });

        priceBox = new VBox(6, AdminUi.fieldLabel("Price (HK$) *"), priceField);
        daysBox = new VBox(6, AdminUi.fieldLabel("Lending duration (days) *"), daysField);

        addPhotoButton.getStyleClass().addAll("btn-outline", "btn-sm");
        addPhotoButton.setOnAction(event -> choosePhotos());

        // ---- prefill for edit -------------------------------------------
        if (existing != null) {
            typeCombo.setValue(typeLabel(existing.getType()));
            if (hasActiveTransaction()) {
                typeCombo.setDisable(true);
                typeCombo.setTooltip(new Tooltip("Resolve active requests and deals before changing the listing type."));
            }
            titleField.setText(existing.getTitle());
            categoryCombo.setValue(existing.getCategory());
            conditionCombo.setValue(existing.getCondition());
            descriptionArea.setText(existing.getDescription());
            priceField.setText(existing.getPrice() == null ? "" : String.valueOf(existing.getPrice()));
            daysField.setText(existing.getLendingDays() == null
                    ? "" : String.valueOf(existing.getLendingDays()));
            meetupCombo.setValue(existing.getMeetupLocation());
            photos.addAll(existing.getPhotoPaths());

            User seller = DummyDataService.getInstance().findUser(existing.getSellerUsername());
            if (seller != null) {
                sellerCombo.getItems().setAll(seller);
                sellerCombo.getSelectionModel().select(seller);
            }
            sellerCombo.setDisable(true);
        } else {
            if (sellers != null) {
                sellerCombo.getItems().setAll(sellers);
            }
        }

        // Show only the price or duration field that belongs to the chosen type.
        typeCombo.valueProperty().addListener((observable, oldValue, newValue) -> updateTypeFields());
        updateTypeFields();

        // ---- layout ------------------------------------------------------
        VBox form = new VBox(12);
        form.setPadding(new Insets(14));
        form.setPrefWidth(540);

        form.getChildren().add(AdminUi.hint(
                "Listing type is chosen first. Required fields depend on the type."));

        if (addMode) {
            form.getChildren().addAll(AdminUi.fieldLabel("Seller (act on behalf of) *"), sellerCombo);
        }

        form.getChildren().addAll(
                AdminUi.fieldLabel("Listing type *"), typeCombo,
                AdminUi.fieldLabel("Item name *"), titleField,
                AdminUi.fieldLabel("Category *"), categoryCombo,
                AdminUi.fieldLabel("Condition *"), conditionCombo,
                AdminUi.fieldLabel("Description *"), descriptionArea,
                priceBox, daysBox,
                AdminUi.fieldLabel("Meet-up location *"), meetupCombo);

        HBox photoHeader = new HBox(10, AdminUi.fieldLabel("Photos (up to 6)"),
                AdminUi.spacer(), photoCountLabel, addPhotoButton);
        photoHeader.setAlignment(Pos.CENTER_LEFT);
        photoPane.setPrefWrapLength(500);
        form.getChildren().addAll(photoHeader, photoPane, errorLabel);

        renderPhotos();

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(590);
        scroll.setStyle("-fx-background-color: transparent;");
        getDialogPane().setContent(scroll);
        getDialogPane().setPrefWidth(600);
        getDialogPane().setPrefHeight(760);

        Node saveButton = getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validateForm()) {
                event.consume();
            }
        });

        setResultConverter(button -> button == save ? buildResult() : null);
    }

    private void updateTypeFields() {
        boolean sell = "Sell".equals(typeCombo.getValue());
        boolean lend = "Lend".equals(typeCombo.getValue());
        priceBox.setVisible(sell);
        priceBox.setManaged(sell);
        daysBox.setVisible(lend);
        daysBox.setManaged(lend);
    }

    private void renderPhotos() {
        photoPane.getChildren().clear();
        if (photos.isEmpty()) {
            photoPane.getChildren().add(AdminUi.muted("No photos selected."));
        } else {
            for (String path : photos) {
                VBox tile = new VBox(4);
                tile.setAlignment(Pos.CENTER);
                tile.getChildren().add(AdminUi.photoThumb(path, 96, 68));

                Label name = new Label(new File(path).getName());
                name.setStyle("-fx-font-size: 10px; -fx-text-fill: #5a5a7a;");
                name.setMaxWidth(96);

                Button remove = new Button("Remove");
                remove.getStyleClass().addAll("btn-outline", "btn-sm");
                remove.setOnAction(event -> {
                    photos.remove(path);
                    renderPhotos();
                });
                tile.getChildren().addAll(name, remove);
                photoPane.getChildren().add(tile);
            }
        }
        photoCountLabel.setText(photos.size() + " / 6 photos");
        addPhotoButton.setDisable(photos.size() >= 6);
    }

    private void choosePhotos() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select listing photos (up to 6)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        List<File> selected = chooser.showOpenMultipleDialog(getOwner());
        if (selected == null) {
            return;
        }

        Set<String> unique = new LinkedHashSet<>(photos);
        int skipped = 0;
        for (File file : selected) {
            if (unique.size() >= 6) {
                skipped++;
                continue;
            }
            unique.add(file.getAbsolutePath());
        }
        photos.clear();
        photos.addAll(unique);
        renderPhotos();

        if (skipped > 0) {
            AdminDialogs.warn("Photo limit reached",
                    "Only 6 photos can be attached. " + skipped + " photo(s) were skipped.");
        }
    }

    private boolean validateForm() {
        AdminUi.clear(errorLabel);

        if (typeCombo.getValue() == null) {
            return fail("Choose a listing type.");
        }
        if (existing != null && selectedType() != existing.getType() && hasActiveTransaction()) {
            return fail("Resolve active requests and deals before changing the listing type.");
        }
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) {
            return fail("Item name is required.");
        }
        if (title.length() > 100) {
            return fail("Item name must be 100 characters or fewer.");
        }
        if (categoryCombo.getValue() == null) {
            return fail("Choose a category.");
        }
        if (conditionCombo.getValue() == null) {
            return fail("Choose a condition.");
        }
        if (meetupCombo.getValue() == null) {
            return fail("Choose a meet-up location.");
        }
        if (descriptionArea.getText() == null || descriptionArea.getText().isBlank()) {
            return fail("Description is required.");
        }
        if (descriptionArea.getText().length() > 2000) {
            return fail("Description must be 2000 characters or fewer.");
        }

        if (existing == null) {
            User seller = sellerCombo.getValue();
            if (seller == null) {
                return fail("Choose the seller this listing is created for.");
            }
            User current = DummyDataService.getInstance().findUser(seller.getUsername());
            if (current == null || !current.isActive() || current.isDeleted()
                    || current.getRole() != com.hkust.marketplace.models.UserRole.SELLER) {
                return fail("The selected seller no longer exists or is not active.");
            }
        }

        String type = typeCombo.getValue();
        if ("Sell".equals(type)) {
            double price;
            try {
                price = Double.parseDouble(priceField.getText().trim());
            } catch (NumberFormatException ex) {
                return fail("A valid price is required for Sell listings.");
            }
            if (!Double.isFinite(price) || price <= 0) {
                return fail("Price must be greater than 0.");
            }
            if (price > 1_000_000) {
                return fail("Price looks too large (maximum HK$1,000,000).");
            }
        } else if ("Lend".equals(type)) {
            int days;
            try {
                days = Integer.parseInt(daysField.getText().trim());
            } catch (NumberFormatException ex) {
                return fail("A valid lending duration is required for Lend listings.");
            }
            if (days < 1 || days > 365) {
                return fail("Lending duration must be between 1 and 365 days.");
            }
        }
        return true;
    }

    private boolean hasActiveTransaction() {
        DummyDataService data = DummyDataService.getInstance();
        return !data.getActiveRequestsForListing(existing.getId()).isEmpty()
                || data.getDealsForListing(existing.getId()).stream().anyMatch(Deal::isActiveOffer);
    }

    private boolean fail(String message) {
        AdminUi.setMessage(errorLabel, message, true);
        return false;
    }

    private Listing buildResult() {
        Listing listing = existing != null ? existing : new Listing();
        listing.setType(selectedType());
        listing.setTitle(titleField.getText().trim());
        listing.setCategory(categoryCombo.getValue());
        listing.setCondition(conditionCombo.getValue());
        listing.setDescription(descriptionArea.getText() == null
                ? "" : descriptionArea.getText().trim());
        listing.setMeetupLocation(meetupCombo.getValue());
        listing.setPhotoPaths(new ArrayList<>(photos));

        switch (selectedType()) {
            case SELL -> {
                listing.setPrice(Double.parseDouble(priceField.getText().trim()));
                listing.setLendingDays(null);
            }
            case LEND -> {
                listing.setLendingDays(Integer.parseInt(daysField.getText().trim()));
                listing.setPrice(null);
            }
            case GIVEAWAY -> {
                listing.setPrice(null);
                listing.setLendingDays(null);
            }
        }

        if (existing == null) {
            User seller = sellerCombo.getValue();
            listing.setSellerUsername(seller.getUsername());
            listing.setSellerFullName(seller.getFullName());
            listing.setStatus(ListingStatus.PENDING_REVIEW);
            listing.setPostedAt(LocalDateTime.now());
        }
        return listing;
    }

    private ListingType selectedType() {
        if (existing != null && typeCombo.getValue() == null) {
            return existing.getType();
        }
        return switch (typeCombo.getValue()) {
            case "Sell" -> ListingType.SELL;
            case "Lend" -> ListingType.LEND;
            default -> ListingType.GIVEAWAY;
        };
    }

    private static String typeLabel(ListingType type) {
        if (type == null) return null;
        return switch (type) {
            case SELL -> "Sell";
            case LEND -> "Lend";
            case GIVEAWAY -> "Give Away";
        };
    }
}
