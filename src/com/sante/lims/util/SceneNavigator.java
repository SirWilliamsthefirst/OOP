package com.sante.lims.util;

import com.sante.lims.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class SceneNavigator {

    private SceneNavigator() {}

    public static void switchTo(Stage stage, String fxmlPath, String title) throws IOException {
        Parent root = FXMLLoader.load(SceneNavigator.class.getResource(fxmlPath));
        stage.setTitle("Sante Diagnostics – " + title);
        stage.setScene(new Scene(root, 1100, 700));
        stage.show();
    }

    public static void openInNewWindow(String fxmlPath, String title) throws IOException {
        Parent root = FXMLLoader.load(SceneNavigator.class.getResource(fxmlPath));
        Stage stage = new Stage();
        stage.setTitle("Sante Diagnostics – " + title);
        stage.setScene(new Scene(root, 900, 600));
        stage.setResizable(false);
        stage.show();
    }

    public static void switchToUserDashboard(Stage stage, User user) throws IOException {
        if (user == null) {
            switchTo(stage, "/fxml/Login.fxml", "Login");
            return;
        }

        switch (user.getRole()) {
            case SUPER_ADMIN -> switchTo(stage, "/fxml/SuperAdminDashboard.fxml", "Super Admin Dashboard");
            case LAB_ATTENDANT -> switchTo(stage, "/fxml/LabAttendantDashboard.fxml", "Lab Attendant Dashboard");
            case CUSTOMER -> switchTo(stage, "/fxml/CustomerDashboard.fxml", "Customer Dashboard");
            default -> switchTo(stage, "/fxml/Login.fxml", "Login");
        }
    }

 
    public static <T> T loadController(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
        loader.load();
        return loader.getController();
    }
}
