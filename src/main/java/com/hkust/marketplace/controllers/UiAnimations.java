package com.hkust.marketplace.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;

/**
 * Small, reusable UI motion helpers.  JavaFX CSS cannot animate transitions, so the
 * common loading/fade/hover effects live here and are shared by all three portals.
 */
public final class UiAnimations {

    private UiAnimations() {
    }

    /** Fades and gently lifts a newly shown page into place. */
    public static void fadeIn(Node node) {
        if (node == null) return;
        node.setOpacity(0);
        node.setTranslateY(10);

        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(260), node);
        slide.setFromY(10);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    /**
     * Replaces the content of a dashboard pane with a short fade/slide transition and a
     * brief loading indicator, so screen changes feel responsive rather than abrupt.
     */
    public static void showPane(StackPane host, Node content) {
        if (host == null || content == null) return;
        host.getChildren().setAll(content);
        fadeIn(content);
        showLoadingIndicator(host);
    }

    private static void showLoadingIndicator(StackPane host) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(34, 34);
        spinner.setMinSize(34, 34);

        StackPane overlay = new StackPane(spinner);
        StackPane.setAlignment(spinner, Pos.CENTER);
        overlay.getStyleClass().add("loading-overlay");
        overlay.setMouseTransparent(true);
        host.getChildren().add(overlay);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(90), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.millis(160));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), overlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> host.getChildren().remove(overlay));

        new javafx.animation.SequentialTransition(fadeIn, hold, fadeOut).play();
    }

    /** Adds a lift/scale effect while the pointer is over a card or row. */
    public static void hoverLift(Node node) {
        if (node == null) return;
        node.getStyleClass().add("interactive-card");
        node.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED, event -> animateScale(node, 1.015, -2));
        node.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_EXITED, event -> animateScale(node, 1.0, 0));
    }

    private static void animateScale(Node node, double scale, double translateY) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(140), node);
        scaleTransition.setToX(scale);
        scaleTransition.setToY(scale);
        TranslateTransition translate = new TranslateTransition(Duration.millis(140), node);
        translate.setToY(translateY);
        new ParallelTransition(scaleTransition, translate).play();
    }

    /** Fades cards in one after another (used for the browse grid). */
    public static void staggerFadeIn(List<? extends Node> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            node.setOpacity(0);
            node.setTranslateY(12);
            PauseTransition delay = new PauseTransition(Duration.millis(Math.min(i * 28, 280)));
            delay.setOnFinished(event -> fadeIn(node));
            delay.play();
        }
    }

    /** A subtle pulse for unread badges / important status updates. */
    public static void pulse(Node node) {
        if (node == null) return;
        ScaleTransition pulse = new ScaleTransition(Duration.millis(260), node);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.14);
        pulse.setToY(1.14);
        pulse.setCycleCount(2);
        pulse.setAutoReverse(true);
        pulse.play();
    }
}
