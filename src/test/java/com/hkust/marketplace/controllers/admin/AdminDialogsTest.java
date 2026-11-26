package com.hkust.marketplace.controllers.admin;

import com.hkust.marketplace.testsupport.FxTest;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AdminDialogsTest {

    @BeforeAll
    static void startFx() {
        FxTest.ensureFx();
    }

    private static void close(DialogPane dialog) {
        FxTest.button(dialog, "OK").fire();
    }

    @Test
    void informationWarningAndErrorDialogsAppear() throws Exception {
        FxTest.run(() -> {
            FxTest.withDialog(() -> AdminDialogs.info("Info", "Message"), AdminDialogsTest::close);
            FxTest.withDialog(() -> AdminDialogs.warn("Warning", "Message"), AdminDialogsTest::close);
            FxTest.withDialog(() -> AdminDialogs.error("Error", "Message"), AdminDialogsTest::close);
        });
    }

    @Test
    void confirmationDialogsReturnTheUsersChoice() throws Exception {
        AtomicBoolean result = new AtomicBoolean();
        FxTest.run(() -> {
            result.set(true);
            FxTest.withDialog(() -> result.set(AdminDialogs.confirm("Confirm?", "Message")),
                    dialog -> FxTest.button(dialog, "Cancel").fire());
            assertFalse(result.get());

            FxTest.withDialog(() -> result.set(AdminDialogs.confirm("Confirm?", "Message")),
                    dialog -> FxTest.button(dialog, "OK").fire());
            assertTrue(result.get());

            FxTest.withDialog(() -> result.set(AdminDialogs.confirmDestructive("Delete?", "Message", "Remove")),
                    dialog -> {
                        assertTrue(FxTest.hasLabel(dialog, "Delete?"));
                        assertEquals("Remove", FxTest.button(dialog, "Remove").getText());
                        FxTest.button(dialog, "Cancel").fire();
                    });
            assertFalse(result.get());

            FxTest.withDialog(() -> result.set(AdminDialogs.confirmDestructive("Delete?", "Message", null)),
                    dialog -> {
                        assertEquals("Confirm", FxTest.button(dialog, "Confirm").getText());
                        FxTest.button(dialog, "Confirm").fire();
                    });
            assertTrue(result.get());
        });
    }

    @Test
    void reasonPromptsRequireNonBlankTextAndTrimTheResult() throws Exception {
        AtomicReference<Optional<String>> result = new AtomicReference<>();
        FxTest.run(() -> {
            FxTest.withDialog(() -> result.set(AdminDialogs.promptReason(
                            "Reject", "Explain why", "Reason (required)")),
                    dialog -> {
                        TextArea area = (TextArea) dialog.lookup(".text-area");
                        assertTrue(FxTest.button(dialog, "Submit").isDisabled());
                        area.setText("   ");
                        assertTrue(FxTest.button(dialog, "Submit").isDisabled());
                        area.setText("  Needs a better photo  ");
                        FxTest.button(dialog, "Submit").fire();
                    });
            assertEquals(Optional.of("Needs a better photo"), result.get());

            FxTest.withDialog(() -> result.set(AdminDialogs.promptReason(
                            "Reject", "Explain why", "Reason (required)")),
                    dialog -> FxTest.button(dialog, "Cancel").fire());
            assertTrue(result.get().isEmpty());
        });
    }
}
