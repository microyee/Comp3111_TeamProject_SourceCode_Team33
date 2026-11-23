package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Buyer;
import com.hkust.marketplace.models.Seller;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.utils.DummyDataService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Task 3.3 – Add / edit user form.
 *
 * <p>Adding requires a unique username, a non-blank full name, a Buyer/Seller role and
 * a password that satisfies the shared strength rule.  Editing exposes the full name
 * only; username and role are read-only and status is changed through the dedicated
 * Deactivate / Reactivate actions.</p>
 */
final class AdminUserFormDialog extends Dialog<User> {

    private final User existing;
    private final TextField usernameField = new TextField();
    private final TextField fullNameField = new TextField();
    private final ComboBox<String> roleCombo = new ComboBox<>(
            FXCollections.observableArrayList("Buyer", "Seller"));
    private final PasswordField passwordField = new PasswordField();
    private final Label errorLabel = AdminUi.errorLabel();

    AdminUserFormDialog(Window owner, User existing) {
        this.existing = existing;
        if (owner != null) {
            initOwner(owner);
        }
        boolean addMode = existing == null;
        setTitle(addMode ? "Add User" : "Edit User");
        setHeaderText(addMode
                ? "Create a new buyer or seller account"
                : "Edit @" + existing.getUsername());

        ButtonType save = new ButtonType(addMode ? "Create User" : "Save Changes",
                ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        VBox form = new VBox(10);
        form.setPadding(new Insets(10));
        form.setPrefWidth(440);

        if (addMode) {
            usernameField.setPromptText("e.g. chan123");
            fullNameField.setPromptText("e.g. Chan Tai Man");
            roleCombo.getSelectionModel().selectFirst();
            passwordField.setPromptText("Initial password");
            form.getChildren().addAll(
                    AdminUi.fieldLabel("Username *"), usernameField,
                    AdminUi.fieldLabel("Full Name *"), fullNameField,
                    AdminUi.fieldLabel("Role *"), roleCombo,
                    AdminUi.fieldLabel("Initial Password *"), passwordField,
                    AdminUi.hint("At least 8 characters with an uppercase letter, "
                            + "a lowercase letter and a digit."),
                    errorLabel);
        } else {
            Label username = new Label(existing.getUsername());
            username.setStyle("-fx-font-weight: 600;");
            Label role = new Label(existing.getRole().name());
            role.setStyle("-fx-font-weight: 600;");
            fullNameField.setText(existing.getFullName());
            form.getChildren().addAll(
                    AdminUi.fieldLabel("Username"), username,
                    AdminUi.fieldLabel("Role"), role,
                    AdminUi.fieldLabel("Full Name *"), fullNameField,
                    AdminUi.hint("Status is managed with Deactivate / Reactivate. "
                            + "Usernames and roles cannot be changed here."),
                    errorLabel);
        }

        getDialogPane().setContent(form);
        getDialogPane().setPrefWidth(480);

        Node saveButton = getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validateForm()) {
                event.consume();
            }
        });

        setResultConverter(button -> button == save ? buildResult() : null);
    }

    private boolean validateForm() {
        AdminUi.clear(errorLabel);
        String fullName = fullNameField.getText() == null ? "" : fullNameField.getText().trim();
        if (fullName.isEmpty()) {
            AdminUi.setMessage(errorLabel, "Full Name cannot be empty.", true);
            return false;
        }

        if (existing != null) {
            return true;
        }

        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        if (username.isEmpty()) {
            AdminUi.setMessage(errorLabel, "Username is required.", true);
            return false;
        }
        if (!username.matches("[A-Za-z0-9._-]{3,30}")) {
            AdminUi.setMessage(errorLabel,
                    "Username must be 3–30 characters and may contain only letters, "
                            + "digits, dot, underscore or hyphen.", true);
            return false;
        }
        if (DummyDataService.getInstance().usernameExists(username)) {
            AdminUi.setMessage(errorLabel, "Username already exists. Choose another.", true);
            return false;
        }
        if (roleCombo.getValue() == null) {
            AdminUi.setMessage(errorLabel, "Choose a role (Buyer or Seller).", true);
            return false;
        }
        String strength = DummyDataService.checkPasswordStrength(passwordField.getText());
        if (!"OK".equals(strength)) {
            AdminUi.setMessage(errorLabel, strength, true);
            return false;
        }
        return true;
    }

    private User buildResult() {
        String fullName = fullNameField.getText().trim();
        if (existing != null) {
            existing.setFullName(fullName);
            return existing;
        }
        String username = usernameField.getText().trim().toLowerCase();
        User user = "Seller".equals(roleCombo.getValue())
                ? new Seller(username, fullName, passwordField.getText())
                : new Buyer(username, fullName, passwordField.getText());
        user.setActive(true);
        return user;
    }
}
