package com.hkust.marketplace.controllers;

import com.hkust.marketplace.utils.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

public class WelcomeController {

    @FXML private BorderPane root;
    @FXML private VBox hero;
    @FXML private HBox roleRow;

    @FXML
    public void initialize() {
        if (hero != null) UiAnimations.fadeIn(hero);
        if (roleRow != null) UiAnimations.staggerFadeIn(new ArrayList<>(roleRow.getChildren()));
    }

    @FXML
    private void goBuyer() {
        System.out.println("[Welcome] goBuyer()");
        SceneManager.showBuyerLogin();
    }

    @FXML
    private void goSeller() {
        System.out.println("[Welcome] goSeller()");
        SceneManager.showSellerLogin();
    }

    @FXML
    private void goAdmin() {
        System.out.println("[Welcome] goAdmin()");
        SceneManager.showAdminLogin();
    }
}
