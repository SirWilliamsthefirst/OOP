package com.sante.lims.controller;

import com.sante.lims.model.User;
import com.sante.lims.service.AuthService;
import com.sante.lims.service.AuthenticationException;
import com.sante.lims.session.SessionManager;
import com.sante.lims.util.SceneNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void onLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Email and password are required.");
            return;
        }

        try {
            User user = authService.authenticate(email, password);
            SessionManager.getInstance().setCurrentUser(user);
            if (user.isForcePwChange()) {
                SceneNavigator.switchTo((Stage) rootPane.getScene().getWindow(), "/fxml/ChangePassword.fxml", "Change Password");
            } else {
                SceneNavigator.switchToUserDashboard((Stage) rootPane.getScene().getWindow(), user);
            }
        } catch (AuthenticationException e) {
            messageLabel.setText(e.getMessage());
        } catch (IOException e) {
            messageLabel.setText("Unable to load UI. " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onRegister(ActionEvent event) {
        try {
            SceneNavigator.openInNewWindow("/fxml/Register.fxml", "Register");
        } catch (IOException e) {
            messageLabel.setText("Unable to open registration screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onVerifyEmail(ActionEvent event) {
        try {
            SceneNavigator.openInNewWindow("/fxml/VerifyEmail.fxml", "Verify Email");
        } catch (IOException e) {
            messageLabel.setText("Unable to open verification screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onExit(ActionEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }
}
