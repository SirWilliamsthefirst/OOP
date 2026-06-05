package com.sante.lims.controller;

import com.sante.lims.service.AuthService;
import com.sante.lims.service.AuthenticationException;
import com.sante.lims.session.SessionManager;
import com.sante.lims.util.SceneNavigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class VerifyEmailController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private TextField emailField;

    @FXML
    private TextField tokenField;

    @FXML
    private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        if (SessionManager.getInstance().isLoggedIn()) {
            String email = SessionManager.getInstance().getCurrentUser().getEmail();
            if (email != null) {
                emailField.setText(email);
            }
        }
    }

    @FXML
    private void onVerify(ActionEvent event) {
        String email = emailField.getText().trim();
        String token = tokenField.getText().trim();

        if (email.isEmpty() || token.isEmpty()) {
            messageLabel.setText("Email and token are required.");
            return;
        }

        try {
            authService.verifyEmail(email, token);
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Email verified successfully. You can now log in.");
        } catch (AuthenticationException e) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void onBack(ActionEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }
}
