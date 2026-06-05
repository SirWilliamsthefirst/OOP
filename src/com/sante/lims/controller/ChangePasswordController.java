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
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class ChangePasswordController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    private User user;
    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        if (user == null) {
            user = SessionManager.getInstance().getCurrentUser();
        }
    }

    @FXML
    private void onSave(ActionEvent event) {
        if (user == null) {
            user = SessionManager.getInstance().getCurrentUser();
            if (user == null) {
                messageLabel.setText("No user information available.");
                return;
            }
        }

        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (newPassword.isBlank() || confirmPassword.isBlank()) {
            messageLabel.setText("Both password fields are required.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        try {
            authService.changePassword(user, newPassword);
            SceneNavigator.switchToUserDashboard((Stage) rootPane.getScene().getWindow(), user);
        } catch (AuthenticationException e) {
            messageLabel.setText(e.getMessage());
        } catch (IOException e) {
            messageLabel.setText("Unable to continue. " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancel(ActionEvent event) throws IOException {
        SceneNavigator.switchToUserDashboard((Stage) rootPane.getScene().getWindow(), SessionManager.getInstance().getCurrentUser());
    }
}
