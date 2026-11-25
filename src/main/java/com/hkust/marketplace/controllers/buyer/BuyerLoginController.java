package com.hkust.marketplace.controllers.buyer;

import com.hkust.marketplace.models.User;
import com.hkust.marketplace.models.UserRole;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.MarketplaceAccounts;
import com.hkust.marketplace.utils.SceneManager;
import com.hkust.marketplace.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class BuyerLoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        if (loginButton != null) loginButton.setDefaultButton(true);
        usernameField.setOnAction(event -> passwordField.requestFocus());
        passwordField.setOnAction(event -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        User user;
        try {
            user = new MarketplaceAccounts(DummyDataService.getInstance()).authenticate(
                    UserRole.BUYER, usernameField.getText(), passwordField.getText());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }

        SessionManager.login(user);
        SceneManager.showBuyerDashboard();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    @FXML
    private void goRegister() {
        SceneManager.showBuyerRegister();
    }

    @FXML
    private void goWelcome() {
        SceneManager.showWelcome();
    }
}
