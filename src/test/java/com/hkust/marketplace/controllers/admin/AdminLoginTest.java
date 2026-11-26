package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Admin;
import com.hkust.marketplace.testsupport.FxTest;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.PasswordUtils;
import com.hkust.marketplace.utils.SceneManager;
import com.hkust.marketplace.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AdminLoginTest {

    @BeforeAll
    static void startFx() {
        FxTest.ensureFx();
    }

    @Test
    void loginValidatesEveryFailureAndSucceedsForTheAdmin() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        Admin admin = (Admin) data.findUser("admin");
        String originalHash = admin.getPasswordHash();
        admin.setPasswordHash(PasswordUtils.hash("admin123"));
        data.registerUser(new Admin("inactive-admin", "Inactive Admin", "Password1"));
        data.findUser("inactive-admin").setActive(false);
        data.registerUser(new Admin("deleted-admin", "Deleted Admin", "Password1"));
        data.findUser("deleted-admin").setDeleted(true);

        FxTest.run(() -> {
            var loader = new FXMLLoader(AdminLoginTest.class.getResource("/fxml/admin/AdminLoginView.fxml"));
            Parent root;
            try {
                root = loader.load();
            } catch (java.io.IOException e) {
                throw new AssertionError(e);
            }
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1200, 780));
            SceneManager.init(stage);
            stage.show();
            AdminLoginController controller = loader.getController();

            TextField username = (TextField) root.lookup("#usernameField");
            PasswordField password = (PasswordField) root.lookup("#passwordField");
            Button login = (Button) root.lookup("#loginButton");
            Label error = (Label) root.lookup("#errorLabel");

            assertTrue(login.isDisabled());
            assertTrue(((Label) root.lookup("#requiredHint")).isVisible());

            username.setText("ghost");
            password.setText("Password1");
            login.fire();
            assertEquals("No account found with that username.", error.getText());

            username.setText("admin");
            password.setText("wrong");
            login.fire();
            assertEquals("Incorrect password. Please try again.", error.getText());

            username.setText("alice");
            password.setText("password1");
            login.fire();
            assertTrue(error.getText().contains("not an Administrator account"));

            username.setText("inactive-admin");
            password.setText("Password1");
            login.fire();
            assertEquals("This administrator account is not active.", error.getText());

            username.setText("deleted-admin");
            password.setText("Password1");
            login.fire();
            assertEquals("This administrator account is not active.", error.getText());

            username.setText("");
            password.setText("");
            try {
                Method handleLogin = AdminLoginController.class.getDeclaredMethod("handleLogin");
                handleLogin.setAccessible(true);
                handleLogin.invoke(controller);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
            assertEquals("Both username and password are required.", error.getText());

            username.setText("admin");
            password.setText("admin123");
            assertFalse(login.isDisabled());
            username.fireEvent(new ActionEvent());
            password.fireEvent(new ActionEvent());
            assertEquals("admin", SessionManager.getCurrentUser().getUsername());
            assertEquals("admin", data.findUser("admin").getUsername());

            Hyperlink back = root.lookupAll(".hyperlink").stream()
                    .filter(Hyperlink.class::isInstance)
                    .map(Hyperlink.class::cast)
                    .findFirst().orElseThrow();
            back.fire();
            stage.hide();
        });
        admin.setPasswordHash(originalHash);
        SessionManager.logout();
    }
}
