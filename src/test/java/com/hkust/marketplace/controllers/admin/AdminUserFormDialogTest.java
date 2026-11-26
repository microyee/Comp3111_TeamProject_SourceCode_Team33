package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Buyer;
import com.hkust.marketplace.models.User;
import com.hkust.marketplace.testsupport.FxTest;
import com.hkust.marketplace.utils.DummyDataService;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AdminUserFormDialogTest {

    @BeforeAll
    static void startFx() {
        FxTest.ensureFx();
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<String> comboWith(javafx.scene.Parent root, String item) {
        return (ComboBox<String>) root.lookupAll(".combo-box").stream()
                .filter(ComboBox.class::isInstance)
                .map(ComboBox.class::cast)
                .filter(combo -> combo.getItems().contains(item))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No combo containing " + item));
    }

    private static String error(javafx.scene.control.DialogPane pane) {
        Label label = (Label) pane.lookup(".error-text");
        return label == null ? null : label.getText();
    }

    @Test
    void addUserValidatesUsernameRoleAndPassword() throws Exception {
        AtomicReference<Optional<User>> result = new AtomicReference<>();
        FxTest.run(() -> {
            Stage owner = new Stage();
            owner.setScene(new javafx.scene.Scene(new javafx.scene.layout.StackPane(), 300, 200));
            AdminUserFormDialog dialog = new AdminUserFormDialog(owner, null);
            FxTest.withDialog(() -> result.set(dialog.showAndWait()), pane -> {
                Button save = FxTest.button(pane, "Create User");

                save.fire();
                assertEquals("Full Name cannot be empty.", error(pane));

                FxTest.field(pane, "e.g. Chan Tai Man").setText("Form User");
                save.fire();
                assertEquals("Username is required.", error(pane));

                FxTest.field(pane, "e.g. chan123").setText("ab");
                save.fire();
                assertTrue(error(pane).contains("3–30 characters"));

                FxTest.field(pane, "e.g. chan123").setText("bad name!");
                save.fire();
                assertTrue(error(pane).contains("3–30 characters"));

                FxTest.field(pane, "e.g. chan123").setText("alice");
                save.fire();
                assertEquals("Username already exists. Choose another.", error(pane));

                FxTest.field(pane, "e.g. chan123").setText("FormUserX");
                ComboBox<String> role = comboWith(pane, "Buyer");
                role.setValue(null);
                save.fire();
                assertEquals("Choose a role (Buyer or Seller).", error(pane));

                role.setValue("Seller");
                save.fire();
                assertEquals("Password cannot be empty.", error(pane));

                ((PasswordField) pane.lookup(".password-field")).setText("weak");
                save.fire();
                assertTrue(error(pane).contains("8 characters"));

                ((PasswordField) pane.lookup(".password-field")).setText("Password1");
                save.fire();
            });
            owner.close();
        });
        User created = result.get().orElseThrow();
        assertEquals("formuserx", created.getUsername());
        assertEquals("Form User", created.getFullName());
        assertTrue(created instanceof com.hkust.marketplace.models.Seller);
        assertTrue(created.isActive());
    }

    @Test
    void editUserValidatesNameAndReturnsTheSameAccount() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        User buyer = new Buyer("form-edit-user", "Original Name", "Password1");
        data.registerUser(buyer);

        AtomicReference<Optional<User>> result = new AtomicReference<>();
        FxTest.run(() -> {
            Stage owner = new Stage();
            owner.setScene(new javafx.scene.Scene(new javafx.scene.layout.StackPane(), 300, 200));
            AdminUserFormDialog dialog = new AdminUserFormDialog(owner, buyer);
            FxTest.withDialog(() -> result.set(dialog.showAndWait()), pane -> {
                FxTest.anyTextField(pane).setText("   ");
                FxTest.button(pane, "Save Changes").fire();
                assertEquals("Full Name cannot be empty.", error(pane));

                FxTest.anyTextField(pane).setText("Updated Name");
                FxTest.button(pane, "Save Changes").fire();
            });
            owner.close();
        });
        User updated = result.get().orElseThrow();
        assertSame(buyer, updated);
        assertEquals("Updated Name", updated.getFullName());
        assertEquals("form-edit-user", updated.getUsername());
    }
}
