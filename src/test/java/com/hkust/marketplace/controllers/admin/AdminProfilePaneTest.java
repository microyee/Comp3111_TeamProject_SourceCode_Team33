package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.User;
import com.hkust.marketplace.testsupport.FxTest;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.PasswordUtils;
import com.hkust.marketplace.utils.SessionManager;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminProfilePaneTest {

    @BeforeAll
    static void startFx() {
        FxTest.ensureFx();
    }

    private static PasswordField password(javafx.scene.Parent root, String prompt) {
        return (PasswordField) root.lookupAll(".password-field").stream()
                .filter(field -> prompt.equals(((PasswordField) field).getPromptText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No password field " + prompt));
    }

    @Test
    void profileValidationAndPasswordChangeCoverEveryBranch() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        User admin = data.findUser("admin");
        String originalName = admin.getFullName();
        String originalHash = admin.getPasswordHash();
        admin.setPasswordHash(PasswordUtils.hash("AdminPass1"));
        SessionManager.login(admin);

        FxTest.run(() -> {
            AdminProfilePane pane = new AdminProfilePane(new AdminDashboardController());
            Stage stage = FxTest.stage(pane);
            TextField fullName = FxTest.field(pane, "Full name *");
            PasswordField current = password(pane, "Current password");
            PasswordField newPassword = password(pane, "New password");
            PasswordField confirm = password(pane, "Confirm new password");
            Button save = FxTest.button(pane, "Save Changes");
            Label error = (Label) pane.lookup(".error-text");
            Label success = (Label) pane.lookup(".success-text");

            fullName.setText("   ");
            save.fire();
            assertEquals("Full Name cannot be empty.", error.getText());

            fullName.setText("x".repeat(81));
            save.fire();
            assertEquals("Full Name must be 80 characters or fewer.", error.getText());

            fullName.setText("Profile Admin");
            save.fire();
            assertEquals("Profile updated successfully.", success.getText());
            assertEquals("Profile Admin", admin.getFullName());

            current.setText("AdminPass1");
            newPassword.setText("");
            confirm.setText("");
            save.fire();
            assertEquals("Enter a new password, or clear the password fields to keep the current one.",
                    error.getText());

            current.setText("");
            newPassword.setText("Password2");
            confirm.setText("");
            save.fire();
            assertEquals("Enter your current password to change it.", error.getText());

            current.setText("wrong");
            newPassword.setText("Password2");
            confirm.setText("Password2");
            save.fire();
            assertEquals("Current password is incorrect.", error.getText());

            current.setText("AdminPass1");
            newPassword.setText("short");
            confirm.setText("short");
            save.fire();
            assertTrue(error.getText().toLowerCase().contains("uppercase")
                    || error.getText().toLowerCase().contains("8"));

            newPassword.setText("NewPass1");
            confirm.setText("NewPass2");
            save.fire();
            assertEquals("New password and confirmation do not match.", error.getText());

            newPassword.setText("NewPass1");
            confirm.setText("NewPass1");
            save.fire();
            assertEquals("Profile updated. Your password was changed and hashed.", success.getText());
            assertTrue(PasswordUtils.verify("NewPass1", admin.getPasswordHash()));
            assertEquals("Profile Admin", admin.getFullName());

            stage.hide();
        });

        try {
            SessionManager.logout();
            FxTest.run(() -> {
                AdminProfilePane anonymous = new AdminProfilePane(new AdminDashboardController());
                Stage stage = FxTest.stage(anonymous);
                FxTest.button(anonymous, "Save Changes").fire();
                assertEquals("No admin session is active. Please log in again.",
                        ((Label) anonymous.lookup(".error-text")).getText());
                stage.hide();
            });
        } finally {
            admin.setFullName(originalName);
            admin.setPasswordHash(originalHash);
            SessionManager.logout();
        }
    }
}
