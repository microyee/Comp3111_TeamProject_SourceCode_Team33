package com.hkust.marketplace.controllers;

import com.hkust.marketplace.models.*;
import com.hkust.marketplace.utils.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public final class PortalUi {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    private PortalUi() {}

    public static String time(LocalDateTime value) {
        return value == null ? "—" : TIME.format(value);
    }

    public static LocalDateTime parseTime(String value) {
        try {
            return LocalDateTime.parse(value.strip().replace(' ', 'T'));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Use YYYY-MM-DD HH:MM:SS for the borrowing times.");
        }
    }

    public static Double number(String value) {
        if (value.isBlank()) return null;
        try {
            return Double.valueOf(value.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid number.");
        }
    }

    public static Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    public static VBox card(Node... children) {
        VBox box = new VBox(8, children);
        box.getStyleClass().add("card");
        return box;
    }

    public static Button button(String text, String style, Runnable work) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        button.setOnAction(event -> run(button, work));
        return button;
    }

    public static void run(Node owner, Runnable work) {
        try {
            work.run();
        } catch (IllegalArgumentException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            style(alert, owner);
            alert.setTitle("Marketplace");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        return scroll;
    }

    public static TextField field(String placeholder) {
        TextField field = new TextField();
        field.setPromptText(placeholder);
        field.setAccessibleText(placeholder);
        return field;
    }

    public static PasswordField password(String placeholder) {
        PasswordField field = new PasswordField();
        field.setPromptText(placeholder);
        field.setAccessibleText(placeholder);
        return field;
    }

    public static <T> ComboBox<T> choice(String placeholder, Collection<T> options) {
        ComboBox<T> combo = new ComboBox<>(FXCollections.observableArrayList(options));
        combo.setPromptText(placeholder);
        combo.setAccessibleText(placeholder);
        return combo;
    }

    public static VBox formField(String caption, Control control) {
        Label label = new Label(caption);
        label.setLabelFor(control);
        control.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, label, control);
    }

    public static void style(Dialog<?> dialog, Node owner) {
        Window window = owner == null || owner.getScene() == null ? SceneManager.getPrimaryStage()
                : owner.getScene().getWindow();
        if (window != null) dialog.initOwner(window);
        dialog.getDialogPane().getStylesheets().add(
                PortalUi.class.getResource("/css/styles.css").toExternalForm());
    }

    public static void form(Node owner, String title, String action, Node fields, Runnable save) {
        Dialog<ButtonType> dialog = new Dialog<>();
        style(dialog, owner);
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        var submit = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
        Label error = label("", "error-text");
        error.setVisible(false);
        error.managedProperty().bind(error.visibleProperty());
        VBox content = new VBox(12, fields, error);
        content.setPrefWidth(440);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(submit, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(submit).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                save.run();
            } catch (IllegalArgumentException e) {
                error.setText(e.getMessage());
                error.setVisible(true);
                event.consume();
            }
        });
        dialog.showAndWait();
    }

    public static boolean confirm(Node owner, String title, String detail) {
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION, detail, ButtonType.OK, ButtonType.CANCEL);
        style(dialog, owner);
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        return dialog.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    public static void textForm(Node owner, String title, String action, String caption, String initial,
                                Consumer<String> save) {
        TextArea area = new TextArea(MarketplaceAccounts.clean(initial));
        area.setWrapText(true);
        area.setPrefRowCount(4);
        form(owner, title, action, formField(caption, area), () -> save.accept(area.getText()));
    }

    public static void termsForm(Node owner, ListingType type, MarketplaceActivity.Terms initial,
                                 String title, Consumer<MarketplaceActivity.Terms> save) {
        switch (type) {
            case SELL -> {
                TextField amount = field("Amount in HKD");
                if (initial.price() != null) amount.setText(initial.price().toString());
                form(owner, title, "Send", formField("Amount in HKD", amount),
                        () -> save.accept(new MarketplaceActivity.Terms(number(amount.getText()), null, null)));
            }
            case LEND -> {
                TextField start = field("YYYY-MM-DD HH:MM:SS");
                TextField end = field("YYYY-MM-DD HH:MM:SS");
                LocalDateTime defaultStart = LocalDateTime.now().plusHours(1).withNano(0);
                start.setText(time(initial.start() == null ? defaultStart : initial.start()));
                end.setText(time(initial.end() == null ? defaultStart.plusHours(1) : initial.end()));
                form(owner, title, "Send", new VBox(12, formField("Start (local time)", start),
                        formField("End (local time)", end)),
                        () -> save.accept(MarketplaceActivity.Terms.loan(parseTime(start.getText()), parseTime(end.getText()))));
            }
            case GIVEAWAY -> {
                if (confirm(owner, title, "Reserve this giveaway while the seller reviews your claim?")) {
                    run(owner, () -> save.accept(MarketplaceActivity.Terms.giveaway()));
                }
            }
        }
    }

    public static Node profile(User user, MarketplaceAccounts accounts, Runnable updated) {
        TextField name = field("Full name");
        name.setText(user.getFullName());
        var residence = choice("Residence hall", MarketplaceAccounts.RESIDENCES);
        residence.setValue(user.getResidenceHall());
        var meetup = choice("Preferred HKUST meet-up location", DummyDataService.HKUST_LOCATIONS);
        meetup.setValue(user.getPreferredMeetup());
        TextArea bio = new TextArea(MarketplaceAccounts.clean(user.getBio()));
        bio.setPrefRowCount(3);
        bio.setWrapText(true);
        PasswordField current = password("Current password");
        PasswordField next = password("New password");
        PasswordField confirm = password("Confirm new password");
        Label status = label("", "muted");
        boolean buyer = user.getRole() == UserRole.BUYER;

        VBox panel = card(label("@" + user.getUsername() + " · " + user.getRole(), "section-title"),
                formField("Full name", name));
        if (buyer) panel.getChildren().add(new HBox(8, formField("Residence hall (optional)", residence),
                button("Clear", "btn-outline", () -> residence.setValue(null))));
        else panel.getChildren().addAll(formField("Bio (optional)", bio),
                new HBox(8, formField("Preferred meet-up (optional)", meetup),
                        button("Clear", "btn-outline", () -> meetup.setValue(null))));
        panel.getChildren().addAll(new Separator(), label("Change password", "section-title"),
                formField("Current password", current), formField("New password", next),
                formField("Confirm new password", confirm), button("Save profile", "btn-primary", () -> {
                    accounts.updateProfile(user, name.getText(), residence.getValue(), bio.getText(), meetup.getValue(),
                            current.getText(), next.getText(), confirm.getText());
                    current.clear();
                    next.clear();
                    confirm.clear();
                    updated.run();
                    status.setText("Profile saved.");
                }), status);
        panel.setMaxWidth(620);
        VBox content = new VBox(panel);
        content.setPadding(new Insets(2, 10, 12, 2));
        return scroll(content);
    }

    public static Node notifications(User user, DummyDataService data, Consumer<Notification> open, Runnable updateBadge) {
        VBox rows = new VBox(10);
        var category = choice("All categories", data.getNotificationsFor(user.getUsername()).stream()
                .map(Notification::getCategory).distinct().sorted().toList());
        CheckBox unread = new CheckBox("Unread only");
        Runnable render = () -> {
            rows.getChildren().clear();
            for (Notification notice : data.getNotificationsFor(user.getUsername())) {
                if ((unread.isSelected() && notice.isRead()) || (category.getValue() != null
                        && !category.getValue().equals(notice.getCategory()))) continue;
                VBox row = card(label(notice.getTitle(), "section-title"), label(notice.getMessage(), "label"),
                        label(notice.getCategory() + " · " + time(notice.getTimestamp()), "muted"));
                row.getStyleClass().add(notice.isRead() ? "notif-read" : "notif-unread");
                HBox actions = new HBox(8);
                if (!notice.isRead()) actions.getChildren().add(button("Mark read", "btn-outline", () -> {
                    data.activity().readNotification(user, notice.getId());
                    updateBadge.run();
                    category.fireEvent(new ActionEvent());
                }));
                if (notice.hasRelatedTarget()) actions.getChildren().add(button("Open", "btn-primary", () -> {
                    data.activity().readNotification(user, notice.getId());
                    updateBadge.run();
                    open.accept(notice);
                }));
                row.getChildren().add(actions);
                rows.getChildren().add(row);
            }
            if (rows.getChildren().isEmpty()) rows.getChildren().add(label("No matching notifications.", "muted"));
        };
        category.setOnAction(event -> render.run());
        unread.setOnAction(event -> render.run());
        Button clear = button("Clear category", "btn-outline", () -> category.setValue(null));
        Button all = button("Mark all read", "btn-primary", () -> {
            data.markAllNotificationsRead(user.getUsername());
            updateBadge.run();
            render.run();
        });
        HBox toolbar = new HBox(8, category, clear, unread, all);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        render.run();
        ScrollPane scroll = scroll(rows);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return new VBox(12, toolbar, scroll);
    }

    public static Node chat(User user, MarketplaceActivity activity, Chat selected, Consumer<Chat> formalRequest) {
        List<Chat> chats = activity.chats(user);
        if (chats.isEmpty()) return card(label("No conversations yet.", "muted"));
        var threads = choice("Conversation", chats);
        threads.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Chat chat) {
                if (chat == null) return "";
                String party = user.getRole() == UserRole.BUYER ? chat.getSellerUsername() : chat.getBuyerUsername();
                return chat.getListingTitle() + " · @" + party;
            }
            @Override public Chat fromString(String text) { return null; }
        });
        threads.setValue(selected == null ? chats.get(0) : selected);
        TextField search = field("Search messages");
        VBox messages = new VBox(8);
        ScrollPane history = scroll(messages);
        VBox.setVgrow(history, Priority.ALWAYS);
        TextArea draft = new TextArea();
        draft.setPromptText("Write a message");
        draft.setAccessibleText("Write a message");
        draft.setWrapText(true);
        draft.setPrefRowCount(2);
        Runnable render = () -> {
            messages.getChildren().clear();
            for (Message message : activity.messages(user, threads.getValue().getId(), search.getText())) {
                messages.getChildren().add(card(label("@" + message.getSenderUsername() + " · " + time(message.getSentAt()), "muted"),
                        label(message.getContent(), "label")));
            }
            if (messages.getChildren().isEmpty()) messages.getChildren().add(label("No matching messages.", "muted"));
            history.layout();
            history.setVvalue(1);
        };
        threads.setOnAction(event -> {
            draft.clear();
            render.run();
        });
        search.textProperty().addListener((obs, old, value) -> render.run());
        HBox actions = new HBox(8, button("Send message", "btn-primary", () -> {
            activity.send(user, threads.getValue().getId(), draft.getText());
            draft.clear();
            search.clear();
            render.run();
        }));
        if (formalRequest != null) {
            actions.getChildren().add(button("Send formal request", "btn-outline", () -> formalRequest.accept(threads.getValue())));
        }
        render.run();
        return new VBox(12, new HBox(8, threads, search), history, draft, actions);
    }
}
