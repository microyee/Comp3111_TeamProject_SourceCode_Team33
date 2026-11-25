package com.hkust.marketplace.utils;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.net.URL;

/**
 * Loads listing photos from either the classpath (generated demo photos) or a local
 * file path chosen by a seller/admin.  Keeping this in one place means the same photo
 * paths work in Buyer, Seller and Admin screens.
 */
public final class ImageSupport {

    private ImageSupport() {
    }

    /** Loads an image, returning null when the path is missing or unreadable. */
    public static Image load(String path, double width, double height) {
        String source = resolve(path);
        if (source == null) return null;
        try {
            Image image = new Image(source, width, height, true, true, false);
            return image.isError() ? null : image;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Loads the original image (used by the zoomable photo viewer). */
    public static Image loadOriginal(String path) {
        String source = resolve(path);
        if (source == null) return null;
        try {
            Image image = new Image(source);
            return image.isError() ? null : image;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String resolve(String path) {
        if (path == null || path.isBlank()) return null;
        if (path.startsWith("/")) {
            URL resource = ImageSupport.class.getResource(path);
            return resource == null ? null : resource.toExternalForm();
        }
        File file = new File(path);
        if (file.isFile()) return file.toURI().toString();
        URL resource = ImageSupport.class.getResource("/" + path);
        return resource == null ? null : resource.toExternalForm();
    }

    /** Returns a rounded photo tile, or a neutral placeholder when unavailable. */
    public static Node thumbnail(String path, double width, double height) {
        StackPane tile = new StackPane();
        tile.setPrefSize(width, height);
        tile.setMinSize(width, height);
        tile.getStyleClass().add("photo-tile");

        Image image = load(path, width, height);
        if (image == null) {
            tile.getStyleClass().add("photo-placeholder");
            Label label = new Label("No photo");
            label.getStyleClass().add("photo-placeholder-label");
            tile.getChildren().add(label);
            return tile;
        }

        ImageView view = new ImageView(image);
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        view.setAccessibleText("Listing photo");
        view.getStyleClass().add("listing-photo");

        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        view.setClip(clip);
        tile.getChildren().add(view);
        return tile;
    }

    /** Returns a photo tile for a listing, using its first photo. */
    public static Node firstPhoto(com.hkust.marketplace.models.Listing listing,
                                  double width, double height) {
        if (listing == null || listing.getPhotoPaths().isEmpty()) {
            return thumbnail("", width, height);
        }
        return thumbnail(listing.getPhotoPaths().get(0), width, height);
    }
}
