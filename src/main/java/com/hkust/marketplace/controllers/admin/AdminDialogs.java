package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.utils.SceneManager;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * Confirmation / information dialogs used by the admin portal.
 *
 * <p>Every destructive action goes through a confirmation dialog, and reason-based
 * actions (reject a listing, take a listing down) cannot be submitted with a blank
 * reason.</p>
 */
final class AdminDialogs {

    private AdminDialogs() {
    }

    static boolean confirm(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm");
        alert.setHeaderText(header);
        alert.setContentText(message);
        prepare(alert);
        return alert.showAndWait()
                .filter(result -> result == ButtonType.OK)
                .isPresent();
    }

    static boolean confirmDestructive(String header, String message, String actionLabel) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        // A warning alert only offers OK by default; destructive actions must also be
        // cancellable so the user can back out without changing anything.
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Confirm destructive action");
        alert.setHeaderText(header);
        alert.setContentText(message);
        prepare(alert);
        Node okButton = alert.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton instanceof Button button) {
            button.setText(actionLabel == null ? "Confirm" : actionLabel);
        }
        return alert.showAndWait()
                .filter(result -> result == ButtonType.OK)
                .isPresent();
    }

    static void info(String header, String message) {
        show(Alert.AlertType.INFORMATION, "Information", header, message);
    }

    static void warn(String header, String message) {
        show(Alert.AlertType.WARNING, "Warning", header, message);
    }

    static void error(String header, String message) {
        show(Alert.AlertType.ERROR, "Error", header, message);
    }

    private static void show(Alert.AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        prepare(alert);
        alert.showAndWait();
    }

    /** Keeps long warning messages readable instead of widening the alert off-screen. */
    private static void prepare(Alert alert) {
        javafx.stage.Stage owner = SceneManager.getPrimaryStage();
        if (owner != null && owner.isShowing() && owner.getScene() != null) {
            alert.initOwner(owner);
        }
        alert.getDialogPane().setPrefWidth(520);
        Node content = alert.getDialogPane().lookup(".content.label");
        if (content instanceof Label label) {
            label.setWrapText(true);
        }
    }

    /**
     * Prompts for a non-empty reason.  The submit button stays disabled until the text
     * area contains a non-blank value, implementing the "reason is mandatory" rule.
     */
    static Optional<String> promptReason(String header, String message, String prompt) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Reason Required");
        dialog.setHeaderText(header);
        javafx.stage.Stage owner = SceneManager.getPrimaryStage();
        if (owner != null && owner.isShowing() && owner.getScene() != null) {
            dialog.initOwner(owner);
        }

        ButtonType submit = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(submit, cancel);

        Label description = new Label(message);
        description.setWrapText(true);
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefRowCount(4);
        area.setWrapText(true);
        Label hint = AdminUi.hint("A written reason is required.");

        VBox content = new VBox(10, description, area, hint);
        content.setPadding(new Insets(8));
        content.setPrefWidth(460);
        dialog.getDialogPane().setContent(content);

        Node submitButton = dialog.getDialogPane().lookupButton(submit);
        submitButton.setDisable(true);
        area.textProperty().addListener((observable, oldValue, newValue) ->
                submitButton.setDisable(newValue == null || newValue.isBlank()));

        dialog.setResultConverter(button -> button == submit ? area.getText().trim() : null);
        return dialog.showAndWait().filter(value -> value != null && !value.isBlank());
    }
}
