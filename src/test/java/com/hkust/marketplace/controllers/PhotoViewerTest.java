package com.hkust.marketplace.controllers;

import com.hkust.marketplace.testsupport.FxTest;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhotoViewerTest {

    @BeforeAll
    static void startFx() {
        FxTest.ensureFx();
    }

    private static String logoPath() throws Exception {
        return Path.of(PhotoViewerTest.class.getResource("/images/hkust_logo.png").toURI())
                .toFile().getAbsolutePath();
    }

    private static DialogPane open(List<String> photos, int index, String title) throws Exception {
        FxTest.run(() -> PhotoViewer.show(null, photos, index, title));
        return FxTest.waitForDialog(8000);
    }

    private static Label countLabel(DialogPane pane) {
        return pane.lookupAll(".photo-viewer-count").stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(label -> label.getText() != null && !label.getText().endsWith("%"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No photo count label"));
    }

    private static Label zoomLabel(DialogPane pane) {
        return pane.lookupAll(".photo-viewer-count").stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(label -> label.getText() != null && label.getText().endsWith("%"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No zoom label"));
    }

    private static void close(DialogPane pane) throws Exception {
        FxTest.run(() -> FxTest.button(pane, "Close").fire());
        FxTest.pump();
    }

    @Test
    void emptyAndUnreadablePhotoSetsShowThePlaceholder() throws Exception {
        DialogPane empty = open(null, 3, "  ");
        FxTest.run(() -> {
            assertEquals("No photo available", FxTest.label(empty, "No photo available").getText());
            assertEquals("No photos", countLabel(empty).getText());
            assertTrue(FxTest.button(empty, "‹").isDisabled());
            assertTrue(FxTest.button(empty, "›").isDisabled());
        });
        close(empty);

        List<String> withBlanks = new ArrayList<>(Arrays.asList("", "   "));
        DialogPane blanks = open(withBlanks, -5, null);
        FxTest.run(() -> assertEquals("No photos", countLabel(blanks).getText()));
        close(blanks);

        DialogPane missing = open(List.of("/images/definitely-not-here.png"), 0, "Missing");
        FxTest.run(() -> assertTrue(countLabel(missing).getText().contains("1 / 1")));
        close(missing);
    }

    @Test
    void navigationZoomKeyboardAndScrollAreInteractive() throws Exception {
        String logo = logoPath();
        DialogPane pane = open(List.of(logo, "/images/hkust_logo.png"), 99, "Listing photos");
        FxTest.run(() -> {
            assertEquals("2 / 2", countLabel(pane).getText());
            assertFalse(FxTest.button(pane, "‹").isDisabled());
            assertTrue(FxTest.button(pane, "›").isDisabled());

            FxTest.button(pane, "‹").fire();
            assertEquals("1 / 2", countLabel(pane).getText());
            assertTrue(FxTest.button(pane, "‹").isDisabled());
            assertFalse(FxTest.button(pane, "›").isDisabled());
            FxTest.button(pane, "›").fire();
            assertEquals("2 / 2", countLabel(pane).getText());

            FxTest.button(pane, "+").fire();
            assertEquals("125%", zoomLabel(pane).getText());
            for (int i = 0; i < 20; i++) {
                FxTest.button(pane, "+").fire();
            }
            assertEquals("400%", zoomLabel(pane).getText());
            for (int i = 0; i < 20; i++) {
                FxTest.button(pane, "−").fire();
            }
            assertEquals("35%", zoomLabel(pane).getText());
            FxTest.button(pane, "Reset").fire();
            assertEquals("100%", zoomLabel(pane).getText());

            ScrollPane scroll = (ScrollPane) pane.lookup(".photo-viewer-scroll");
            scroll.fireEvent(scrollEvent(120));
            assertNotEquals("100%", zoomLabel(pane).getText());
            scroll.fireEvent(scrollEvent(-120));
            scroll.fireEvent(scrollEvent(0));

            pane.fireEvent(key(KeyCode.LEFT));
            pane.fireEvent(key(KeyCode.RIGHT));
            pane.fireEvent(key(KeyCode.EQUALS));
            pane.fireEvent(key(KeyCode.PLUS));
            pane.fireEvent(key(KeyCode.MINUS));
            pane.fireEvent(key(KeyCode.DIGIT0));
            assertEquals("100%", zoomLabel(pane).getText());
            assertEquals("2 / 2", countLabel(pane).getText());
        });
        close(pane);
    }

    @Test
    void singlePhotoDisablesBothNavigationButtons() throws Exception {
        DialogPane pane = open(List.of(logoPath()), 0, "One");
        FxTest.run(() -> {
            assertEquals("1 / 1", countLabel(pane).getText());
            assertTrue(FxTest.button(pane, "‹").isDisabled());
            assertTrue(FxTest.button(pane, "›").isDisabled());
            FxTest.button(pane, "+").fire();
            FxTest.button(pane, "−").fire();
            FxTest.button(pane, "Reset").fire();
        });
        close(pane);
    }

    @Test
    void interactiveTargetWalksTheNodeParentChain() throws Exception {
        FxTest.run(() -> {
            assertFalse(PhotoViewer.isInteractiveTarget(null));
            assertTrue(PhotoViewer.isInteractiveTarget(new Button("b")));
            assertTrue(PhotoViewer.isInteractiveTarget(new Hyperlink("h")));
            assertTrue(PhotoViewer.isInteractiveTarget(new TextField()));
            assertTrue(PhotoViewer.isInteractiveTarget(new ComboBox<String>()));
            Label tile = new Label("tile");
            tile.getStyleClass().add("photo-tile");
            assertTrue(PhotoViewer.isInteractiveTarget(tile));
            Label image = new Label("image");
            image.getStyleClass().add("photo-viewer-image");
            assertTrue(PhotoViewer.isInteractiveTarget(image));

            Label child = new Label("child");
            StackPane wrapper = new StackPane(child);
            assertFalse(PhotoViewer.isInteractiveTarget(child));
            StackPane plain = new StackPane(new Label("plain"));
            assertFalse(PhotoViewer.isInteractiveTarget(plain.getChildren().get(0)));
            assertFalse(PhotoViewer.isInteractiveTarget(new StackPane()));
        });
    }

    private static ScrollEvent scrollEvent(double deltaY) {
        return new ScrollEvent(ScrollEvent.SCROLL, 0, 0, 0, 0,
                false, false, false, false, false, false,
                0, deltaY, 0, deltaY,
                ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null);
    }

    private static KeyEvent key(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code,
                false, false, false, false);
    }
}
