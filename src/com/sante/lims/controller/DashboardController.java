package com.sante.lims.controller;

import com.sante.lims.model.User;
import com.sante.lims.session.SessionManager;
import com.sante.lims.util.SceneNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label statusLabel;

    private User currentUser;

    @FXML
    private void initialize() {
        if (currentUser == null) {
            currentUser = SessionManager.getInstance().getCurrentUser();
        }
        updateView();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        SessionManager.getInstance().setCurrentUser(user);
        updateView();
    }

    private void updateView() {
        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getFullName());
            roleLabel.setText("Role: " + currentUser.getRole());
            statusLabel.setText("You are logged in as " + currentUser.getRole() + ". Use the navigation panel to continue.");
        }
    }

    @FXML
    private void onLogout(ActionEvent event) throws IOException {
        SessionManager.getInstance().logout();
        Stage stage = (Stage) rootPane.getScene().getWindow();
        SceneNavigator.switchTo(stage, "/fxml/Login.fxml", "Login");
    }
}
