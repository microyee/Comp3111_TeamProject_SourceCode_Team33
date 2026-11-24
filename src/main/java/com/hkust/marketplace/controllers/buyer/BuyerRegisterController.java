package com.hkust.marketplace.controllers.buyer;

import com.hkust.marketplace.models.UserRole;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SceneManager;
import com.hkust.marketplace.utils.MarketplaceAccounts;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class BuyerRegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> residenceCombo;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    @FXML
    public void initialize() {
        residenceCombo.setItems(FXCollections.observableArrayList(MarketplaceAccounts.RESIDENCES));
    }

    @FXML
    private void handleRegister() {
        hideMessages();

        try {
            new MarketplaceAccounts(DummyDataService.getInstance()).register(UserRole.BUYER,
                    usernameField.getText(), fullNameField.getText(), passwordField.getText(),
                    confirmPasswordField.getText(), residenceCombo.getValue(), null, null);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }

        showSuccess("Registration successful! You can now login.");
        // Clear form
        usernameField.clear();
        fullNameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        residenceCombo.getSelectionModel().clearSelection();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    @FXML private void goLogin() { SceneManager.showBuyerLogin(); }
    @FXML private void goWelcome() { SceneManager.showWelcome(); }
}
