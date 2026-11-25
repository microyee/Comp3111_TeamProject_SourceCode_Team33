package com.hkust.marketplace.controllers;

import com.hkust.marketplace.utils.ImageSupport;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Modal photo viewer with zoom controls, mouse-wheel zoom and previous/next navigation.
 * Opened by clicking any listing photo in the buyer/seller/admin screens.
 */
public final class PhotoViewer {

    private static final double VIEW_WIDTH = 900;
    private static final double VIEW_HEIGHT = 530;
    private static final double MIN_ZOOM = 0.35;
    private static final double MAX_ZOOM = 4.0;

    private PhotoViewer() {
    }

    public static void show(Node owner, List<String> photoPaths, int initialIndex, String title) {
        List<String> copy = photoPaths == null ? List.of() : List.copyOf(photoPaths);
        // Defer the modal so a click that arrives during a page/hover animation cannot
        // call showAndWait during animation or layout processing.
        Platform.runLater(() -> showNow(owner, copy, initialIndex, title));
    }

    private static void showNow(Node owner, List<String> photoPaths, int initialIndex, String title) {
        List<String> photos = new ArrayList<>();
        if (photoPaths != null) {
            for (String path : photoPaths) {
                if (path != null && !path.isBlank()) photos.add(path);
            }
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        PortalUi.style(dialog, owner);
        dialog.setTitle("View photos");
        dialog.setHeaderText(null);
        dialog.setResizable(true);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(1040, 760);
        dialog.getDialogPane().setMinSize(760, 560);
        dialog.getDialogPane().getStyleClass().add("photo-viewer-dialog");

        int[] index = {Math.max(0, Math.min(initialIndex, Math.max(0, photos.size() - 1)))};
        double[] baseSize = {0, 0};
        double[] zoom = {1.0};

        Label titleLabel = new Label(title == null || title.isBlank() ? "Listing photos" : title);
        titleLabel.getStyleClass().add("section-title");
        Label count = new Label();
        count.getStyleClass().add("photo-viewer-count");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(12, titleLabel, headerSpacer, count);
        header.setAlignment(Pos.CENTER_LEFT);

        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.getStyleClass().add("photo-viewer-image");

        Label placeholder = new Label("No photo available");
        placeholder.getStyleClass().add("muted");
        StackPane canvas = new StackPane(view, placeholder);
        canvas.getStyleClass().add("photo-viewer-canvas");
        canvas.setAlignment(Pos.CENTER);

        ScrollPane scroll = new ScrollPane(canvas);
        scroll.setPannable(true);
        scroll.getStyleClass().add("photo-viewer-scroll");
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Label zoomLabel = new Label("100%");
        zoomLabel.getStyleClass().add("photo-viewer-count");

        Button previous = new Button("‹");
        previous.getStyleClass().addAll("btn-outline", "btn-sm");
        Button next = new Button("›");
        next.getStyleClass().addAll("btn-outline", "btn-sm");
        Button zoomOut = new Button("−");
        zoomOut.getStyleClass().addAll("btn-outline", "btn-sm");
        Button reset = new Button("Reset");
        reset.getStyleClass().addAll("btn-outline", "btn-sm");
        Button zoomIn = new Button("+");
        zoomIn.getStyleClass().addAll("btn-outline", "btn-sm");

        Runnable render = () -> {
            Image image = photos.isEmpty() ? null : ImageSupport.loadOriginal(photos.get(index[0]));
            view.setImage(image);
            boolean hasImage = image != null;
            view.setVisible(hasImage);
            placeholder.setVisible(!hasImage);
            count.setText(photos.isEmpty() ? "No photos" : (index[0] + 1) + " / " + photos.size());
            previous.setDisable(index[0] <= 0);
            next.setDisable(photos.isEmpty() || index[0] >= photos.size() - 1);
            zoomLabel.setText(Math.round(zoom[0] * 100) + "%");

            if (!hasImage) {
                canvas.setPrefSize(640, 420);
                canvas.setMinSize(640, 420);
                return;
            }

            double fit = Math.min(VIEW_WIDTH / image.getWidth(), VIEW_HEIGHT / image.getHeight());
            fit = Math.min(fit, 1.0);
            baseSize[0] = image.getWidth() * fit;
            baseSize[1] = image.getHeight() * fit;
            applyZoom(view, canvas, baseSize, zoom[0]);
        };

        previous.setOnAction(event -> {
            if (index[0] > 0) {
                index[0]--;
                zoom[0] = 1.0;
                render.run();
            }
        });
        next.setOnAction(event -> {
            if (index[0] < photos.size() - 1) {
                index[0]++;
                zoom[0] = 1.0;
                render.run();
            }
        });
        zoomOut.setOnAction(event -> {
            zoom[0] = clamp(zoom[0] * 0.8);
            render.run();
        });
        zoomIn.setOnAction(event -> {
            zoom[0] = clamp(zoom[0] * 1.25);
            render.run();
        });
        reset.setOnAction(event -> {
            zoom[0] = 1.0;
            render.run();
        });
        scroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) return;
            zoom[0] = clamp(zoom[0] * (event.getDeltaY() > 0 ? 1.1 : 0.9));
            render.run();
            event.consume();
        });
        dialog.getDialogPane().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT) previous.fire();
            else if (event.getCode() == KeyCode.RIGHT) next.fire();
            else if (event.getCode() == KeyCode.EQUALS || event.getCode() == KeyCode.PLUS) zoomIn.fire();
            else if (event.getCode() == KeyCode.MINUS) zoomOut.fire();
            else if (event.getCode() == KeyCode.DIGIT0) reset.fire();
        });

        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8, previous, next, toolbarSpacer, zoomOut, zoomLabel, reset, zoomIn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("photo-viewer-toolbar");

        VBox content = new VBox(12, header, scroll, toolbar);
        content.setPadding(new Insets(6));
        content.getStyleClass().add("photo-viewer");
        dialog.getDialogPane().setContent(content);
        render.run();
        dialog.showAndWait();
    }

    private static void applyZoom(ImageView view, StackPane canvas, double[] baseSize, double zoom) {
        double width = baseSize[0] * zoom;
        double height = baseSize[1] * zoom;
        view.setFitWidth(width);
        view.setFitHeight(height);
        canvas.setPrefSize(Math.max(width + 48, 640), Math.max(height + 48, 420));
        canvas.setMinSize(Math.max(width + 48, 640), Math.max(height + 48, 420));
    }

    private static double clamp(double zoom) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
    }

    /**
     * True for buttons, text inputs, combo boxes and photo tiles.  Used by listing
     * rows to distinguish a real row click from a click on an embedded control.
     */
    public static boolean isInteractiveTarget(Object target) {
        if (!(target instanceof Node start)) return false;
        Node node = start;
        while (node != null) {
            if (node instanceof ButtonBase || node instanceof Hyperlink
                    || node instanceof TextInputControl || node instanceof ComboBoxBase<?>) {
                return true;
            }
            if (node.getStyleClass().contains("photo-tile")
                    || node.getStyleClass().contains("photo-viewer-image")) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }
}
