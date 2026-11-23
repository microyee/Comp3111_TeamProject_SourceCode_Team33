package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Notification;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.models.UserRole;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.PasswordUtils;
import com.hkust.marketplace.utils.SceneManager;
import com.hkust.marketplace.utils.SessionManager;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Task 3.1 – Admin Login.
 *
 * <p>Only the seeded administrator account can use this screen.  A valid buyer or
 * seller credential is explicitly rejected with a "wrong account type" message, and
 * empty fields keep the submit button disabled.</p>
 */
public class AdminLoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label requiredHint;
    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        if (loginButton != null) {
            var fieldsEmpty = Bindings.createBooleanBinding(
                    () -> usernameField.getText().trim().isEmpty()
                            || passwordField.getText().isEmpty(),
                    usernameField.textProperty(), passwordField.textProperty());
            loginButton.disableProperty().bind(fieldsEmpty);
            if (requiredHint != null) {
                requiredHint.visibleProperty().bind(fieldsEmpty);
                requiredHint.managedProperty().bind(fieldsEmpty);
            }
        }
        if (passwordField != null) {
            passwordField.setOnAction(event -> {
                if (loginButton == null || !loginButton.isDisabled()) {
                    handleLogin();
                }
            });
        }
    }

    @FXML
    private void handleLogin() {
        hideError();

        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Both username and password are required.");
            return;
        }

        DummyDataService data = DummyDataService.getInstance();
        User user = data.findUser(username);

        if (user == null) {
            showError("No account found with that username.");
            return;
        }
        if (!PasswordUtils.verify(password, user.getPasswordHash())) {
            showError("Incorrect password. Please try again.");
            return;
        }
        if (user.getRole() != UserRole.ADMIN) {
            showError("This account is not an Administrator account. "
                    + "Please use the Buyer or Seller portal.");
            return;
        }
        if (user.isDeleted() || !user.isActive()) {
            showError("This administrator account is not active.");
            return;
        }

        SessionManager.login(user);
        data.logAudit(user.getUsername(), "LOGIN", "USER", user.getUsername(),
                "Signed in to the Admin Portal");
        data.addNotification(new Notification(user.getUsername(), "Admin Signed In",
                "You signed in to the Admin Portal.", "Account"));
        SceneManager.showAdminDashboard();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void goWelcome() {
        SceneManager.showWelcome();
    }
}
