package com.hkust.marketplace.testsupport;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Small JavaFX test harness shared by the UI tests.  It starts the toolkit once,
 * runs work on the FX thread, and provides helpers for locating controls inside a
 * scene or a modal dialog.
 */
public final class FxTest {

    private static boolean started;

    private FxTest() {
    }

    public static synchronized void ensureFx() {
        if (started) {
            return;
        }
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        if (os.contains("linux") && System.getenv("DISPLAY") == null) {
            throw new org.opentest4j.TestAbortedException(
                    "JavaFX tests require a display. On Linux, use xvfb-run.");
        }
        try {
            CountDownLatch ready = new CountDownLatch(1);
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                ready.countDown();
            });
            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX toolkit did not start");
            }
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit was started by an earlier test class.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        started = true;
    }

    /** Runs work on the JavaFX application thread and waits for it to finish. */
    public static void run(Runnable work) throws Exception {
        ensureFx();
        FutureTask<Void> task = new FutureTask<>(work, null);
        Platform.runLater(task);
        task.get(90, TimeUnit.SECONDS);
    }

    /** Runs work on the JavaFX application thread and returns its result. */
    public static <T> T call(Callable<T> work) throws Exception {
        ensureFx();
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(90, TimeUnit.SECONDS);
    }

    /** Waits until all previously queued FX events have been processed. */
    public static void pump() throws Exception {
        run(() -> {
        });
    }

    public static Stage stage(Parent root) {
        Stage stage = new Stage();
        Scene scene = new Scene(root, 1200, 800);
        var css = FxTest.class.getResource("/css/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.show();
        return stage;
    }

    public static void layout(Parent root) {
        root.applyCss();
        root.layout();
        if (root.getScene() != null) {
            root.getScene().getRoot().applyCss();
            root.getScene().getRoot().layout();
        }
    }

    public static Button button(Parent root, String text) {
        layout(root);
        return root.lookupAll(".button").stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(b -> b.getText() != null && text.equalsIgnoreCase(b.getText().strip()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No button with text " + text + " in "
                        + root.lookupAll(".button").stream()
                        .map(node -> node instanceof Button b ? "[" + b.getText() + "]" : node.toString())
                        .toList()));
    }

    public static TextField field(Parent root, String promptOrAccessibleText) {
        layout(root);
        return root.lookupAll(".text-field").stream()
                .filter(TextField.class::isInstance)
                .map(TextField.class::cast)
                .filter(f -> promptOrAccessibleText.equals(f.getPromptText())
                        || promptOrAccessibleText.equals(f.getAccessibleText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No text field named " + promptOrAccessibleText));
    }

    public static TextField anyTextField(Parent root) {
        layout(root);
        return root.lookupAll(".text-field").stream()
                .filter(TextField.class::isInstance)
                .map(TextField.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No text field"));
    }

    public static TextArea area(Parent root, String promptOrAccessibleText) {
        layout(root);
        return root.lookupAll(".text-area").stream()
                .filter(TextArea.class::isInstance)
                .map(TextArea.class::cast)
                .filter(a -> promptOrAccessibleText.equals(a.getPromptText())
                        || promptOrAccessibleText.equals(a.getAccessibleText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No text area named " + promptOrAccessibleText));
    }

    public static TextArea anyArea(Parent root) {
        layout(root);
        return root.lookupAll(".text-area").stream()
                .filter(TextArea.class::isInstance)
                .map(TextArea.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No text area"));
    }

    public static ComboBox<?> combo(Parent root, String promptOrAccessibleText) {
        layout(root);
        return root.lookupAll(".combo-box").stream()
                .filter(ComboBox.class::isInstance)
                .map(ComboBox.class::cast)
                .filter(c -> promptOrAccessibleText.equals(c.getPromptText())
                        || promptOrAccessibleText.equals(c.getAccessibleText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No combo box named " + promptOrAccessibleText));
    }

    @SuppressWarnings("unchecked")
    public static <T> ComboBox<T> combo(Parent root, String promptOrAccessibleText, Class<T> type) {
        return (ComboBox<T>) combo(root, promptOrAccessibleText);
    }

    public static Labeled label(Parent root, String text) {
        layout(root);
        return root.lookupAll(".label").stream()
                .filter(Labeled.class::isInstance)
                .map(Labeled.class::cast)
                .filter(l -> text.equals(l.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No label with text " + text));
    }

    public static boolean hasLabel(Parent root, String text) {
        layout(root);
        return root.lookupAll(".label").stream()
                .filter(Labeled.class::isInstance)
                .map(Labeled.class::cast)
                .anyMatch(l -> text.equals(l.getText()));
    }

    public static DialogPane visibleDialog() {
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .map(window -> window.getScene() == null ? null : window.getScene().getRoot())
                .filter(DialogPane.class::isInstance)
                .map(DialogPane.class::cast)
                .reduce((first, last) -> last)
                .orElseThrow(() -> new AssertionError("No visible dialog"));
    }

    /** Waits until a modal dialog becomes visible (used with deferred dialogs). */
    public static DialogPane waitForDialog(long timeoutMillis) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline) {
            DialogPane pane = call(() -> {
                try {
                    return visibleDialog();
                } catch (AssertionError none) {
                    return null;
                }
            });
            if (pane != null) {
                return pane;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("No dialog appeared within " + timeoutMillis + " ms");
    }

    public static void closeDialogs() {
        for (Window window : List.copyOf(Window.getWindows())) {
            if (window.isShowing() && window.getScene() != null
                    && window.getScene().getRoot() instanceof DialogPane) {
                window.hide();
            }
        }
    }

    /**
     * Fires a control that is expected to open a modal dialog and applies the given
     * steps to each dialog that appears (outermost first).  A step may fire the
     * dialog's own buttons; the following step runs against the next dialog.
     */
    @SafeVarargs
    public static void withDialogs(Button trigger, Consumer<DialogPane>... steps) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        schedule(steps, 0, error);
        trigger.fire();
        if (error.get() != null) {
            throw new AssertionError("Dialog interaction failed", error.get());
        }
    }

    /** Runs a blocking dialog action while applying the given interaction steps. */
    @SafeVarargs
    public static void withDialog(Runnable showAction, Consumer<DialogPane>... steps) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        schedule(steps, 0, error);
        showAction.run();
        if (error.get() != null) {
            closeDialogs();
            throw new AssertionError("Dialog interaction failed", error.get());
        }
    }

    private static void schedule(Consumer<DialogPane>[] steps, int index, AtomicReference<Throwable> error) {
        Platform.runLater(() -> {
            if (error.get() != null) {
                return;
            }
            try {
                DialogPane dialog = visibleDialog();
                if (index + 1 < steps.length) {
                    schedule(steps, index + 1, error);
                }
                steps[index].accept(dialog);
            } catch (Throwable e) {
                error.set(e);
                closeDialogs();
            }
        });
    }

    /** Fills every visible text input with the given value (used by simple forms). */
    public static void fillTextInputs(Parent root, String value) {
        layout(root);
        for (Node node : root.lookupAll(".text-input")) {
            if (node instanceof TextInputControl input) {
                input.setText(value);
            }
        }
    }

    /** Returns the buttons inside a dialog, in dialog-button order. */
    public static List<Button> dialogButtons(DialogPane pane) {
        List<Button> buttons = new ArrayList<>(pane.getButtonTypes().stream()
                .map(pane::lookupButton)
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .toList());
        return buttons;
    }
}
