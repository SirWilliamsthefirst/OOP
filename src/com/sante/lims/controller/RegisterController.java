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

public class RegisterController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    private final AuthService authService = new AuthService();

   @FXML
    private void onRegister(ActionEvent event) {
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("All fields are required.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Passwords do not match.");
            return;
        }
       try {
            User user = authService.registerCustomer(fullName, email, password);
            SessionManager.getInstance().setCurrentUser(user);
            messageLabel.setStyle("-fx-text-fill: #43a047;");
            messageLabel.setText("Registration successful! Redirecting to email verification...");
            Stage stage = (Stage) rootPane.getScene().getWindow();
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            pause.setOnFinished(e -> {
                try {
                    SceneNavigator.switchTo(stage, "/fxml/VerifyEmail.fxml", "Verify Email");
                } catch (IOException ex) {
                    messageLabel.setStyle("-fx-text-fill: red;");
                    messageLabel.setText("Unable to continue. " + ex.getMessage());
                }
            });
            pause.play();
        } catch (AuthenticationException e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void onBackToLogin(ActionEvent event) {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            SceneNavigator.switchTo(stage, "/fxml/Login.fxml", "Login");
        } catch (IOException e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Unable to go back. " + e.getMessage());
        }
    }
}