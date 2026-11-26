package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.Notification;
import com.hkust.marketplace.testsupport.FxTest;
import com.hkust.marketplace.utils.DummyDataService;
import com.hkust.marketplace.utils.SceneManager;
import com.hkust.marketplace.utils.SessionManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminDashboardTest {

    @BeforeAll
    static void startFx() {
        FxTest.ensureFx();
    }

    private static FXMLLoader loader() {
        return new FXMLLoader(AdminDashboardTest.class.getResource("/fxml/admin/AdminDashboardView.fxml"));
    }

    private static Parent load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void navigationAndNotificationRoutesCoverEveryScreen() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        SessionManager.login(data.findUser("admin"));
        data.addNotification(new Notification("admin", "Unread test", "Message", "System"));
        data.logAudit("admin", "TEST", "SYSTEM", "none", "Coverage");
        FxTest.run(() -> {
            FXMLLoader loader = loader();
            Parent root = load(loader);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1200, 780));
            SceneManager.init(stage);
            stage.show();
            AdminDashboardController controller = loader.getController();

            controller.refreshUnreadBadge();
            controller.refreshWelcome();
            controller.openNotificationTarget(null, null);
            controller.openNotificationTarget("REVIEW", "R1");
            controller.openNotificationTarget("LISTINGS", "L1");
            controller.openNotificationTarget("USERS", "alice");
            controller.openNotificationTarget("DEALS", "D1");
            controller.openNotificationTarget("RATINGS", "R1");
            controller.openNotificationTarget("REPORTS", "P1");
            controller.openNotificationTarget("SOMETHING_ELSE", null);
            controller.showAllListings((String) null);
            controller.showAllListings("missing");
            controller.showUsers((String) null);
            controller.showUsers("alice");
            controller.showRatings((String) null);
            controller.showRatings("alice");
            controller.openNotificationTarget("REVIEW", null);

            FxTest.withDialogs(FxTest.button(stage.getScene().getRoot(), "Logout"),
                    dialog -> FxTest.button(dialog, "Cancel").fire());
            assertNotNull(SessionManager.getCurrentUser());

            FxTest.withDialogs(FxTest.button(stage.getScene().getRoot(), "Logout"),
                    dialog -> FxTest.button(dialog, "OK").fire());
            assertNull(SessionManager.getCurrentUser());
            stage.hide();
        });

        FxTest.run(() -> {
            SessionManager.logout();
            FXMLLoader loader = loader();
            Parent root = load(loader);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1200, 780));
            SceneManager.init(stage);
            stage.show();
            AdminDashboardController controller = loader.getController();
            controller.refreshUnreadBadge();
            controller.refreshWelcome();
            Stage primary = SceneManager.getPrimaryStage();
            if (primary != null) {
                primary.hide();
            }
            stage.hide();
        });
    }

    @Test
    void dashboardRejectsANonAdminSession() throws Exception {
        DummyDataService data = DummyDataService.getInstance();
        SessionManager.login(data.findUser("alice"));
        FxTest.run(() -> {
            FXMLLoader loader = loader();
            Parent root = load(loader);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1200, 780));
            SceneManager.init(stage);
            stage.show();
            assertNull(SessionManager.getCurrentUser());
            stage.hide();
        });
        FxTest.pump();
        FxTest.run(() -> {
            Stage stage = SceneManager.getPrimaryStage();
            if (stage != null) {
                stage.hide();
            }
        });
        SessionManager.logout();
    }
}
