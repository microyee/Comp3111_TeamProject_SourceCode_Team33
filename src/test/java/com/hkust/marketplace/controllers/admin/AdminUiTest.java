package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.models.ListingStatus;
import com.hkust.marketplace.models.UserRole;
import com.hkust.marketplace.testsupport.FxTest;
import com.hkust.marketplace.utils.SceneManager;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AdminUiTest {

    @BeforeAll
    static void startFx() {
        FxTest.ensureFx();
    }

    @Test
    void textBadgesCoverEveryStatusRoleAndCategory() throws Exception {
        FxTest.run(() -> {
            for (ListingStatus status : ListingStatus.values()) {
                assertNotNull(AdminUi.statusText(status));
                assertNotNull(AdminUi.statusStyle(status));
                assertNotNull(AdminUi.statusShortText(status));
                assertNotNull(AdminUi.statusBadge(status));
            }
            assertEquals("Unknown", AdminUi.statusText(null));
            assertEquals("Unknown", AdminUi.statusShortText(null));
            assertEquals("badge-removed", AdminUi.statusStyle(null));

            for (UserRole role : UserRole.values()) {
                assertNotNull(AdminUi.roleBadge(role));
            }
            assertEquals("Unknown", ((Label) AdminUi.roleBadge(null)).getText());
            assertEquals("Active", ((Label) AdminUi.userStatusBadge(true)).getText());
            assertEquals("Deactivated", ((Label) AdminUi.userStatusBadge(false)).getText());

            assertTrue(AdminUi.dealStatusBadge("Pending").getStyleClass().contains("badge-pending"));
            assertTrue(AdminUi.dealStatusBadge("Accepted").getStyleClass().contains("badge-live"));
            assertTrue(AdminUi.dealStatusBadge("Completed").getStyleClass().contains("badge-sold"));
            assertTrue(AdminUi.dealStatusBadge("Returned").getStyleClass().contains("badge-removed"));
            assertTrue(AdminUi.dealStatusBadge("Cancelled").getStyleClass().contains("badge-rejected"));
            assertEquals("Unknown", AdminUi.dealStatusBadge(null).getText());
            assertFalse(AdminUi.dealStatusBadge("Something else").getStyleClass().contains("badge-pending"));

            assertTrue(AdminUi.categoryBadge("Submission").getStyleClass().contains("badge"));
            assertTrue(AdminUi.categoryBadge("Report").getStyleClass().contains("badge"));
            assertTrue(AdminUi.categoryBadge("Account").getStyleClass().contains("badge"));
            assertTrue(AdminUi.categoryBadge("Offer").getStyleClass().contains("badge"));
            assertTrue(AdminUi.categoryBadge("System").getStyleClass().contains("badge"));
            assertEquals("System", AdminUi.categoryBadge(null).getText());

            assertEquals("?", AdminUi.initials(null));
            assertEquals("?", AdminUi.initials("   "));
            assertEquals("A", AdminUi.initials("alice"));
            assertEquals("AC", AdminUi.initials("Alice Chan"));
            assertEquals("AC", AdminUi.initials("Alice B Chan"));

            assertEquals("—", AdminUi.formatDate(null));
            assertEquals("14 Jul 2030", AdminUi.formatDate(LocalDate.of(2030, 7, 14)));
            assertEquals("—", AdminUi.formatDateTime(null));
            assertEquals("14 Jul 2030 12:00",
                    AdminUi.formatDateTime(LocalDateTime.of(2030, 7, 14, 12, 0)));
        });
    }

    @Test
    void messagesBadgesAndLayoutHelpersHandleNullAndBlank() throws Exception {
        FxTest.run(() -> {
            Label label = AdminUi.errorLabel();
            AdminUi.setMessage(label, null, true);
            assertFalse(label.isVisible());
            assertFalse(label.isManaged());
            AdminUi.setMessage(label, "   ", false);
            assertFalse(label.isVisible());
            AdminUi.setMessage(label, "Saved", false);
            assertTrue(label.isVisible());
            assertTrue(label.isManaged());
            assertTrue(label.getStyleClass().contains("success-text"));
            AdminUi.clear(label);
            assertEquals("", label.getText());
            assertFalse(label.isVisible());

            assertNotNull(AdminUi.badge("Plain", null));
            assertNotNull(AdminUi.title("Title"));
            assertNotNull(AdminUi.fieldLabel("Field"));
            assertNotNull(AdminUi.muted("Muted"));
            assertNotNull(AdminUi.hint("Hint"));
            assertNotNull(AdminUi.successLabel());
            assertNotNull(AdminUi.spacer());
            assertNotNull(AdminUi.toolbar(AdminUi.spacer()));
            assertNotNull(AdminUi.emptyState("Nothing", "Nothing here"));
            assertNotNull(AdminUi.scroll(new StackPane()));
            assertNotNull(AdminUi.photoThumb("/images/hkust_logo.png", 40, 30));
            assertNotNull(AdminUi.photoThumb("", 40, 30));

            AdminUi.window(null);
            StackPane orphan = new StackPane();
            AdminUi.window(orphan);

            Stage stage = new Stage();
            SceneManager.init(stage);
            stage.setScene(new Scene(new StackPane(), 300, 200));
            StackPane attached = new StackPane();
            stage.getScene().setRoot(attached);
            assertSame(stage, AdminUi.window(attached));
            Stage other = new Stage();
            other.setScene(new Scene(new StackPane(), 300, 200));
            other.show();
            SceneManager.init(other);
            assertSame(other, AdminUi.window(new StackPane()));
            other.close();
            stage.close();
        });
    }
}
