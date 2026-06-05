package com.sante.lims.controller;

import com.sante.lims.dao.ResultDAO;
import com.sante.lims.dao.SampleEventDAO;
import com.sante.lims.dao.TestRequestDAO;
import com.sante.lims.dao.UserDAO;
import com.sante.lims.model.Result;
import com.sante.lims.model.TestRequest;
import com.sante.lims.model.User;
import com.sante.lims.service.EmailService;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LabAttendantDashboardController {

    @FXML private Button navQueue, navResults;
    @FXML private VBox paneQueue, paneResults;
    @FXML private Label welcomeLabel, pageTitleLabel;
    @FXML private AnchorPane rootPane;

    // ── Queue ────────────────────────────────────────────────────
    @FXML private TableView<TestRequest> queueTable;
    @FXML private TableColumn<TestRequest, String> colCustomer, colTest, colPayment,
            colStatus, colDate, colPay, colSample;
    @FXML private Label queueMsg;

    // ── Upload Results ───────────────────────────────────────────
    @FXML private ComboBox<String> requestCombo, resultFormatCombo;
    @FXML private VBox uploadForm, numericBox, textBox, fileBox;
    @FXML private TextField numericField, filePathField;
    @FXML private TextArea textResultField;
    @FXML private Label uploadRequestInfo, uploadMsg;

    private final TestRequestDAO requestDAO = new TestRequestDAO();
    private final ResultDAO resultDAO       = new ResultDAO();
    private final SampleEventDAO sampleDAO  = new SampleEventDAO();
    private final UserDAO userDAO           = new UserDAO();

    private List<TestRequest> allRequests;
    private TestRequest selectedRequest;
    private File chosenFile;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML
    private void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) welcomeLabel.setText("Welcome, " + user.getFullName());

        resultFormatCombo.setItems(FXCollections.observableArrayList("NUMERIC", "TEXT", "PDF", "IMAGE"));
        resultFormatCombo.setOnAction(e -> switchResultInput());

        setupQueueTable();
        loadQueue();
    }

    // ── Navigation ───────────────────────────────────────────────

    @FXML private void onNavQueue(ActionEvent e)   { showPane("queue"); }
    @FXML private void onNavResults(ActionEvent e) { showPane("results"); loadRequestCombo(); }

    private void showPane(String name) {
        paneQueue.setVisible(false);   paneQueue.setManaged(false);
        paneResults.setVisible(false); paneResults.setManaged(false);
        switch (name) {
            case "queue"   -> { paneQueue.setVisible(true);   paneQueue.setManaged(true);   pageTitleLabel.setText("Request Queue"); navQueue.getStyleClass().add("nav-btn-active"); navResults.getStyleClass().remove("nav-btn-active"); }
            case "results" -> { paneResults.setVisible(true); paneResults.setManaged(true); pageTitleLabel.setText("Upload Results"); navResults.getStyleClass().add("nav-btn-active"); navQueue.getStyleClass().remove("nav-btn-active"); }
        }
    }

    // ── Request Queue ────────────────────────────────────────────

    private void setupQueueTable() {
        colCustomer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerName()));
        colTest.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getTestTypeName()));
        colPayment.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getPaymentStatus().name()));
        colStatus.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getStatus().name().replace("_", " ")));
        colDate.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(FMT) : ""));

        // Mark Paid button
        colPay.setCellFactory(col -> new TableCell<>() {
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
                    UUID labId = SessionManager.getInstance().getCurrentUser().getId();
                    if (requestDAO.markPaid(req.getId(), labId)) {
                        AuditLogger.log("MARK_PAID", "test_requests", req.getId(), "Paid: " + req.getTestTypeName() + " / " + req.getCustomerName());
                        loadQueue();
                    } else { queueMsg.setText("Failed to mark paid."); }
                });
                setGraphic(btn);
            }
        });

        // Sample status button
        colSample.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Next Stage");
            { btn.getStyleClass().add("btn-secondary"); btn.setStyle("-fx-font-size:11px;"); }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                TestRequest req = getTableView().getItems().get(getIndex());
                String next = nextStatus(req.getStatus());
                if (next == null) { Label lbl = new Label("Done"); lbl.setStyle("-fx-text-fill:#888;"); setGraphic(lbl); return; }
                btn.setText(next.replace("_", " "));
                btn.setOnAction(e -> {
                    UUID labId = SessionManager.getInstance().getCurrentUser().getId();
                    requestDAO.updateStatus(req.getId(), next);
                    sampleDAO.record(req.getId(), toSampleEvent(next), null, labId);
                    AuditLogger.log("UPDATE_SAMPLE_STATUS", "test_requests", req.getId(), "Status → " + next);
                    loadQueue();
                });
                setGraphic(btn);
            }
        });
    }

    private String nextStatus(TestRequest.Status current) {
        return switch (current) {
            case PENDING           -> "SAMPLE_COLLECTED";
            case SAMPLE_COLLECTED  -> "PROCESSING";
            case PROCESSING        -> "VALIDATING";
            default                -> null;
        };
    }

    private String toSampleEvent(String requestStatus) {
        return switch (requestStatus) {
            case "SAMPLE_COLLECTED" -> "COLLECTED";
            case "PROCESSING"       -> "PROCESSING";
            case "VALIDATING"       -> "VALIDATED";
            default                 -> requestStatus;
        };
    }

    @FXML private void onRefreshQueue(ActionEvent e) { loadQueue(); }

    private void loadQueue() {
        allRequests = requestDAO.findAll();
        queueTable.setItems(FXCollections.observableArrayList(allRequests));
        queueMsg.setText("");
    }

    // ── Upload Results ───────────────────────────────────────────

    private void loadRequestCombo() {
        allRequests = requestDAO.findAll();
        requestCombo.getItems().clear();
        for (TestRequest r : allRequests) {
            requestCombo.getItems().add(r.getCustomerName() + " – " + r.getTestTypeName() + " (" + r.getStatus().name() + ")");
        }
        uploadForm.setVisible(false); uploadForm.setManaged(false);
    }

    @FXML private void onLoadRequest(ActionEvent e) {
        int idx = requestCombo.getSelectionModel().getSelectedIndex();
        if (idx < 0 || allRequests == null || idx >= allRequests.size()) {
            uploadMsg.setText("Please select a request."); return;
        }
        selectedRequest = allRequests.get(idx);
        uploadRequestInfo.setText("Patient: " + selectedRequest.getCustomerName() + "  |  Test: " + selectedRequest.getTestTypeName() + "  |  Status: " + selectedRequest.getStatus().name().replace("_", " "));
        resultFormatCombo.setValue(null);
        numericBox.setVisible(false); numericBox.setManaged(false);
        textBox.setVisible(false);    textBox.setManaged(false);
        fileBox.setVisible(false);    fileBox.setManaged(false);
        uploadMsg.setText("");
        uploadForm.setVisible(true); uploadForm.setManaged(true);
    }

    private void switchResultInput() {
        String fmt = resultFormatCombo.getValue();
        numericBox.setVisible(false); numericBox.setManaged(false);
        textBox.setVisible(false);    textBox.setManaged(false);
        fileBox.setVisible(false);    fileBox.setManaged(false);
        if (fmt == null) return;
        switch (fmt) {
            case "NUMERIC" -> { numericBox.setVisible(true); numericBox.setManaged(true); }
            case "TEXT"    -> { textBox.setVisible(true);    textBox.setManaged(true); }
            case "PDF", "IMAGE" -> { fileBox.setVisible(true); fileBox.setManaged(true); }
        }
    }

    @FXML private void onBrowseFile(ActionEvent e) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Result File");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PDF & Images", "*.pdf", "*.png", "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        chosenFile = fc.showOpenDialog(filePathField.getScene().getWindow());
        if (chosenFile != null) filePathField.setText(chosenFile.getAbsolutePath());
    }

    @FXML private void onUploadResult(ActionEvent e) {
        uploadMsg.setText("");
        if (selectedRequest == null) { uploadMsg.setText("No request selected."); return; }
        String fmt = resultFormatCombo.getValue();
        if (fmt == null) { uploadMsg.setText("Select a result format."); return; }

        UUID labId = SessionManager.getInstance().getCurrentUser().getId();
        BigDecimal numVal = null;
        String textVal = null, filePath = null;

        switch (fmt) {
            case "NUMERIC" -> {
                try { numVal = new BigDecimal(numericField.getText().trim()); }
                catch (NumberFormatException ex) { uploadMsg.setText("Enter a valid number."); return; }
            }
            case "TEXT" -> {
                textVal = textResultField.getText();
                if (textVal.isBlank()) { uploadMsg.setText("Result text cannot be empty."); return; }
            }
            case "PDF", "IMAGE" -> {
                if (chosenFile == null) { uploadMsg.setText("Please choose a file."); return; }
                filePath = chosenFile.getAbsolutePath();
            }
        }

        Optional<Result> result = resultDAO.upload(selectedRequest.getId(), fmt, numVal, textVal, filePath, labId);
        if (result.isPresent()) {
            AuditLogger.log("UPLOAD_RESULT", "results", result.get().getId(), "Uploaded for: " + selectedRequest.getCustomerName() + " / " + selectedRequest.getTestTypeName());
            uploadMsg.setStyle("-fx-text-fill:#43a047;");
            uploadMsg.setText("Result uploaded. Click 'Validate & Notify' to release to patient.");
        } else {
            uploadMsg.setText("Upload failed. Try again.");
        }
    }

    @FXML private void onValidateResult(ActionEvent e) {
        uploadMsg.setText("");
        if (selectedRequest == null) { uploadMsg.setText("No request selected."); return; }

        Optional<Result> optResult = resultDAO.findByRequestId(selectedRequest.getId());
        if (optResult.isEmpty()) { uploadMsg.setText("Upload a result first before validating."); return; }

        Result result = optResult.get();
        UUID labId = SessionManager.getInstance().getCurrentUser().getId();

        if (resultDAO.validate(result.getId(), labId)) {
            // Set request to COMPLETED and set result_ready_at
            requestDAO.setResultReadyAt(selectedRequest.getId(), LocalDateTime.now());
            AuditLogger.log("VALIDATE_RESULT", "results", result.getId(), "Validated for: " + selectedRequest.getCustomerName());

            // Send email notification
            userDAO.findById(selectedRequest.getCustomerId()).ifPresent(customer -> {
                try {
                    EmailService.sendResultReadyEmail(customer, selectedRequest.getTestTypeName());
                    resultDAO.markNotificationSent(result.getId());
                } catch (Exception ex) {
                    System.err.println("[LabAttendant] Email notify failed: " + ex.getMessage());
                }
            });

            uploadMsg.setStyle("-fx-text-fill:#43a047;");
            uploadMsg.setText("✔ Result validated and patient notified by email.");
        } else {
            uploadMsg.setText("Validation failed. Result may already be validated.");
        }
    }

    @FXML private void onLogout(ActionEvent e) throws IOException {
        AuditLogger.log("LOGOUT", "Lab Attendant logged out.");
        SessionManager.getInstance().logout();
        SceneNavigator.switchTo((Stage) navQueue.getScene().getWindow(), "/fxml/Login.fxml", "Login");
    }
}