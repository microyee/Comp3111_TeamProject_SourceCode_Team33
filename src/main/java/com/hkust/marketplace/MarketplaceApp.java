package com.hkust.marketplace;

import com.hkust.marketplace.utils.SceneManager;
import com.hkust.marketplace.utils.DummyDataService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.util.Duration;
import javafx.application.Application;
import javafx.stage.Stage;

/** Entry point for the JavaFX campus marketplace. */
public class MarketplaceApp extends Application {
    public static final EventType<Event> LOANS_UPDATED = new EventType<>(Event.ANY, "LOANS_UPDATED");
    private Timeline loanUpdates;

    @Override
    public void start(Stage primaryStage) {
        SceneManager.init(primaryStage);
        SceneManager.showWelcome();
        primaryStage.setTitle("HKUST Marketplace");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.show();
        loanUpdates = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (DummyDataService.getInstance().activity().advanceLoans()) {
                Node target = primaryStage.getScene().lookup("#contentPane");
                if (target != null) target.fireEvent(new Event(LOANS_UPDATED));
            }
        }));
        loanUpdates.setCycleCount(Animation.INDEFINITE);
        loanUpdates.play();
    }

    @Override
    public void stop() {
        if (loanUpdates != null) loanUpdates.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
