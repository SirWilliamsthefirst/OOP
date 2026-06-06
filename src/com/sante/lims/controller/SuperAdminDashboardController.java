package com.sante.lims.controller;

import com.sante.lims.dao.TestTypeDAO;
import com.sante.lims.dao.TestRequestDAO;
import com.sante.lims.dao.UserDAO;
import com.sante.lims.model.TestRequest;
import com.sante.lims.model.TestType;
import com.sante.lims.model.User;
import com.sante.lims.service.AuthenticationException;
import com.sante.lims.session.SessionManager;
import com.sante.lims.util.AuditLogger;
import com.sante.lims.util.SceneNavigator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import com.sante.lims.util.DBConnection;

public class SuperAdminDashboardController {

    // Sidebar nav buttons
    @FXML private Button navTests, navQueue, navUsers, navAudit;

    // Panes 
    @FXML private VBox paneTests, paneQueue, paneUsers, paneAudit;

    // Top bar
    @FXML private Label welcomeLabel, pageTitleLabel;

    //Test catalogue
    @FXML private TableView<TestType> testTable;
    @FXML private TableColumn<TestType, String> colTestName, colTestCategory,
            colTestPrice, colTestTat, colTestFormat, colTestAction;
    @FXML private VBox addTestForm;
    @FXML private TextField testNameField, testCategoryField, testPriceField, testTatField;
    @FXML private ComboBox<String> testFormatCombo;
    @FXML private TextArea testDescField;
    @FXML private Label testFormMsg;

    // Request queue 
    @FXML private TableView<TestRequest> queueTable;
    @FXML private TableColumn<TestRequest, String> colQCustomer, colQTest, colQPayment,
            colQStatus, colQDate, colQAction;
    @FXML private Label queueMsg;

    //Manage users
    @FXML private TextField newUserName, newUserEmail;
    @FXML private ComboBox<String> newUserRole;
    @FXML private Label userFormMsg;

    //Audit trail
    @FXML private TableView<String[]> auditTable;
    @FXML private TableColumn<String[], String> colAuditTime, colAuditUser,
            colAuditAction, colAuditEntity, colAuditDetail;

    @FXML private AnchorPane rootPane;

    private final TestTypeDAO testTypeDAO   = new TestTypeDAO();
    private final TestRequestDAO requestDAO = new TestRequestDAO();
    private final UserDAO userDAO           = new UserDAO();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML
    private void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getFullName());
        }
        testFormatCombo.setItems(FXCollections.observableArrayList("NUMERIC", "TEXT", "PDF", "IMAGE"));
        newUserRole.setItems(FXCollections.observableArrayList("LAB_ATTENDANT", "CUSTOMER"));

        setupTestTable();
        setupQueueTable();
        setupAuditTable();
        loadTests();
    }

    //Navigation

    @FXML private void onNavTests(ActionEvent e)  { showPane("tests"); }
    @FXML private void onNavQueue(ActionEvent e)  { showPane("queue"); loadQueue(); }
    @FXML private void onNavUsers(ActionEvent e)  { showPane("users"); }
    @FXML private void onNavAudit(ActionEvent e)  { showPane("audit"); loadAudit(); }

    private void showPane(String name) {
        paneTests.setVisible(false); paneTests.setManaged(false);
        paneQueue.setVisible(false); paneQueue.setManaged(false);
        paneUsers.setVisible(false); paneUsers.setManaged(false);
        paneAudit.setVisible(false); paneAudit.setManaged(false);

        switch (name) {
            case "tests" -> { paneTests.setVisible(true); paneTests.setManaged(true); pageTitleLabel.setText("Test Catalogue"); }
            case "queue" -> { paneQueue.setVisible(true); paneQueue.setManaged(true); pageTitleLabel.setText("Request Queue"); }
            case "users" -> { paneUsers.setVisible(true); paneUsers.setManaged(true); pageTitleLabel.setText("Manage Users"); }
            case "audit" -> { paneAudit.setVisible(true); paneAudit.setManaged(true); pageTitleLabel.setText("Audit Trail"); }
        }
        // highlight active nav
        navTests.getStyleClass().remove("nav-btn-active"); navQueue.getStyleClass().remove("nav-btn-active");
        navUsers.getStyleClass().remove("nav-btn-active"); navAudit.getStyleClass().remove("nav-btn-active");
        switch (name) {
            case "tests" -> navTests.getStyleClass().add("nav-btn-active");
            case "queue" -> navQueue.getStyleClass().add("nav-btn-active");
            case "users" -> navUsers.getStyleClass().add("nav-btn-active");
            case "audit" -> navAudit.getStyleClass().add("nav-btn-active");
        }
    }

    // Test Catalogue

    private void setupTestTable() {
        colTestName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colTestCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        colTestPrice.setCellValueFactory(c -> new SimpleStringProperty("₦" + c.getValue().getPrice().toPlainString()));
        colTestTat.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getTatHours())));
        colTestFormat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getResultFormat().name()));
        colTestAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Deactivate");
            { btn.getStyleClass().add("btn-danger"); btn.setStyle("-fx-font-size:11px;"); }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                TestType tt = getTableView().getItems().get(getIndex());
                btn.setOnAction(e -> {
                    testTypeDAO.deactivate(tt.getId());
                    AuditLogger.log("DEACTIVATE_TEST", "test_types", tt.getId(), "Deactivated: " + tt.getName());
                    loadTests();
                });
                setGraphic(btn);
            }
        });
    }

    private void loadTests() {
        testTable.setItems(FXCollections.observableArrayList(testTypeDAO.findAll()));
    }

    @FXML private void onAddTest(ActionEvent e) {
        addTestForm.setVisible(true); addTestForm.setManaged(true);
        testFormMsg.setText("");
    }

    @FXML private void onCancelAddTest(ActionEvent e) {
        addTestForm.setVisible(false); addTestForm.setManaged(false);
        clearTestForm();
    }

    @FXML private void onSaveTest(ActionEvent e) {
        testFormMsg.setText("");
        String name     = testNameField.getText().trim();
        String category = testCategoryField.getText().trim();
        String priceStr = testPriceField.getText().trim();
        String tatStr   = testTatField.getText().trim();
        String format   = testFormatCombo.getValue();

        if (name.isEmpty() || category.isEmpty() || priceStr.isEmpty() || tatStr.isEmpty() || format == null) {
            testFormMsg.setText("All fields except description are required."); return;
        }
        BigDecimal price; int tat;
        try { price = new BigDecimal(priceStr); } catch (NumberFormatException ex) {
            testFormMsg.setText("Price must be a valid number."); return; }
        try { tat = Integer.parseInt(tatStr); } catch (NumberFormatException ex) {
            testFormMsg.setText("TAT must be a whole number."); return; }

        UUID adminId = SessionManager.getInstance().getCurrentUser().getId();
        testTypeDAO.create(name, category, price, tat, format, testDescField.getText(), adminId)
                .ifPresentOrElse(t -> {
                    AuditLogger.log("CREATE_TEST", "test_types", t.getId(), "Created test: " + t.getName());
                    testFormMsg.setStyle("-fx-text-fill: #43a047;");
                    testFormMsg.setText("Test '" + name + "' created successfully.");
                    loadTests();
                    clearTestForm();
                    addTestForm.setVisible(false); addTestForm.setManaged(false);
                }, () -> testFormMsg.setText("Failed to save test. Check for duplicate name."));
    }

    private void clearTestForm() {
        testNameField.clear(); testCategoryField.clear(); testPriceField.clear();
        testTatField.clear(); testFormatCombo.setValue(null); testDescField.clear();
    }

    // Request Queue

    private void setupQueueTable() {
        colQCustomer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerName()));
        colQTest.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTestTypeName()));
        colQPayment.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentStatus().name()));
        colQStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name().replace("_", " ")));
        colQDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(FMT) : ""));
        colQAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Mark Paid");
            { btn.getStyleClass().add("btn-success"); btn.setStyle("-fx-font-size:11px;"); }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                TestRequest req = getTableView().getItems().get(getIndex());
                if (req.getPaymentStatus() == TestRequest.PaymentStatus.PAID) {
                    Label lbl = new Label("✔ Paid"); lbl.setStyle("-fx-text-fill:#43a047; -fx-font-weight:bold;");
                    setGraphic(lbl); return;
                }
                btn.setOnAction(e -> {
                    UUID adminId = SessionManager.getInstance().getCurrentUser().getId();
                    if (requestDAO.markPaid(req.getId(), adminId)) {
                        AuditLogger.log("MARK_PAID", "test_requests", req.getId(), "Marked paid: " + req.getTestTypeName() + " for " + req.getCustomerName());
                        loadQueue();
                    } else {
                        queueMsg.setText("Failed to mark as paid.");
                    }
                });
                setGraphic(btn);
            }
        });
    }

    @FXML private void onRefreshQueue(ActionEvent e) { loadQueue(); }

    private void loadQueue() {
        queueTable.setItems(FXCollections.observableArrayList(requestDAO.findAll()));
        queueMsg.setText("");
    }

    // Manage Users

    @FXML private void onCreateUser(ActionEvent e) {
        userFormMsg.setText("");
        String name  = newUserName.getText().trim();
        String email = newUserEmail.getText().trim();
        String role  = newUserRole.getValue();

        if (name.isEmpty() || email.isEmpty() || role == null) {
            userFormMsg.setText("All fields are required."); return;
        }
        UUID adminId = SessionManager.getInstance().getCurrentUser().getId();
        try {
            userDAO.createStaffUser(name, email, User.Role.valueOf(role), adminId);
            AuditLogger.log("CREATE_USER", "users", null, "Created " + role + ": " + email);
            userFormMsg.setStyle("-fx-text-fill:#43a047;");
            userFormMsg.setText("Account created. Temporary password sent to: " + email);
            newUserName.clear(); newUserEmail.clear(); newUserRole.setValue(null);
        } catch (AuthenticationException ex) {
            userFormMsg.setText(ex.getMessage());
        }
    }

    // Audit Trail

    private void setupAuditTable() {
        colAuditTime.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue()[0]));
        colAuditUser.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue()[1]));
        colAuditAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));
        colAuditEntity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[3]));
        colAuditDetail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[4]));
    }

    @FXML private void onRefreshAudit(ActionEvent e) { loadAudit(); }

    private void loadAudit() {
        String sql = """
            SELECT a.logged_at, u.full_name, a.action, a.entity_type, a.detail
            FROM audit_log a LEFT JOIN users u ON u.id = a.user_id
            ORDER BY a.logged_at DESC LIMIT 500
            """;
        var rows = FXCollections.<String[]>observableArrayList();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getObject("logged_at") != null ? rs.getTimestamp("logged_at").toLocalDateTime().format(FMT) : "",
                    rs.getString("full_name") != null ? rs.getString("full_name") : "System",
                    rs.getString("action"),
                    rs.getString("entity_type") != null ? rs.getString("entity_type") : "",
                    rs.getString("detail") != null ? rs.getString("detail") : ""
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        auditTable.setItems(rows);
    }

    // Logout

    @FXML private void onLogout(ActionEvent e) throws IOException {
        AuditLogger.log("LOGOUT", "Super admin logged out.");
        SessionManager.getInstance().logout();
        SceneNavigator.switchTo((Stage) navTests.getScene().getWindow(), "/fxml/Login.fxml", "Login");
    }
}