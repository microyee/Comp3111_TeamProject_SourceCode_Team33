package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.User;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.PasswordUtils;
import com.hkust.marketplace.utils.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Task 3.4 – Manage Own Profile.
 *
 * <p>The admin can change their full name and, optionally, their password.  A password
 * change requires the current password, a new password that passes the shared strength
 * rule, and a matching confirmation.  New passwords are stored as salted PBKDF2
 * hashes, never as plaintext.</p>
 */
final class AdminProfilePane extends VBox {

    private final AdminDashboardController host;
    private final User user;

    private final TextField fullNameField = new TextField();
    private final PasswordField currentPasswordField = new PasswordField();
    private final PasswordField newPasswordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final Label errorLabel = AdminUi.errorLabel();
    private final Label successLabel = AdminUi.successLabel();
    private final Button saveButton = new Button("Save Changes");

    AdminProfilePane(AdminDashboardController host) {
        super(12);
        this.host = host;
        this.user = SessionManager.getCurrentUser();
        setPadding(new Insets(4));

        VBox form = new VBox(10);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(18));
        form.setMaxWidth(540);

        Label title = new Label("Admin Profile");
        title.getStyleClass().add("section-title");

        Label username = new Label(user == null ? "—" : user.getUsername());
        username.setStyle("-fx-font-weight: 600;");
        Label role = new Label("Administrator");
        role.setStyle("-fx-font-weight: 600;");
        Label memberSince = new Label(user == null || user.getCreatedAt() == null
                ? "—" : AdminUi.formatDate(user.getCreatedAt().toLocalDate()));

        Label intro = AdminUi.muted("Your username and role are fixed. You may update your "
                + "display name and change your password below.");
        intro.setWrapText(true);

        form.getChildren().addAll(title, intro,
                AdminUi.fieldLabel("Username"), username,
                AdminUi.fieldLabel("Role"), role,
                AdminUi.fieldLabel("Member since"), memberSince);

        fullNameField.setText(user == null ? "" : user.getFullName());
        fullNameField.setPromptText("Full name *");
        form.getChildren().addAll(AdminUi.fieldLabel("Full Name *"), fullNameField);

        Label passwordHeading = new Label("Change Password (optional)");
        passwordHeading.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; "
                + "-fx-text-fill: #003974; -fx-padding: 8 0 0 0;");

        currentPasswordField.setPromptText("Current password");
        newPasswordField.setPromptText("New password");
        confirmPasswordField.setPromptText("Confirm new password");

        form.getChildren().addAll(passwordHeading,
                AdminUi.hint("Leave all three password fields blank to keep the current password. "
                        + "A new password must contain at least 8 characters, an uppercase letter, "
                        + "a lowercase letter and a digit."),
                AdminUi.fieldLabel("Current Password"), currentPasswordField,
                AdminUi.fieldLabel("New Password"), newPasswordField,
                AdminUi.fieldLabel("Confirm New Password"), confirmPasswordField,
                errorLabel, successLabel);

        saveButton.getStyleClass().add("btn-success");
        saveButton.setOnAction(event -> saveProfile());

        HBox actions = new HBox(10, saveButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        form.getChildren().add(actions);

        ScrollPane scroll = AdminUi.scroll(form);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
    }

    private void saveProfile() {
        AdminUi.clear(errorLabel, successLabel);

        if (user == null) {
            AdminUi.setMessage(errorLabel, "No admin session is active. Please log in again.", true);
            return;
        }

        String fullName = fullNameField.getText() == null ? "" : fullNameField.getText().trim();
        if (fullName.isEmpty()) {
            AdminUi.setMessage(errorLabel, "Full Name cannot be empty.", true);
            return;
        }
        if (fullName.length() > 80) {
            AdminUi.setMessage(errorLabel, "Full Name must be 80 characters or fewer.", true);
            return;
        }

        String current = currentPasswordField.getText() == null ? "" : currentPasswordField.getText();
        String newPassword = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();
        boolean changingPassword = !current.isEmpty() || !newPassword.isEmpty() || !confirm.isEmpty();

        if (changingPassword) {
            if (newPassword.isEmpty()) {
                AdminUi.setMessage(errorLabel,
                        "Enter a new password, or clear the password fields to keep the current one.",
                        true);
                return;
            }
            if (current.isEmpty()) {
                AdminUi.setMessage(errorLabel, "Enter your current password to change it.", true);
                return;
            }
            if (!PasswordUtils.verify(current, user.getPasswordHash())) {
                AdminUi.setMessage(errorLabel, "Current password is incorrect.", true);
                return;
            }
            String strength = DummyDataService.checkPasswordStrength(newPassword);
            if (!"OK".equals(strength)) {
                AdminUi.setMessage(errorLabel, strength, true);
                return;
            }
            if (!newPassword.equals(confirm)) {
                AdminUi.setMessage(errorLabel,
                        "New password and confirmation do not match.", true);
                return;
            }
        }

        user.setFullName(fullName);
        if (changingPassword) {
            user.setPasswordHash(PasswordUtils.hash(newPassword));
        }
        DummyDataService.getInstance().updateUser(user);
        DummyDataService.getInstance().logAudit(user.getUsername(), "UPDATE_PROFILE",
                "USER", user.getUsername(),
                changingPassword ? "Updated full name and password" : "Updated full name");

        AdminUi.setMessage(successLabel,
                changingPassword
                        ? "Profile updated. Your password was changed and hashed."
                        : "Profile updated successfully.",
                false);
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        host.refreshWelcome();
    }
}
