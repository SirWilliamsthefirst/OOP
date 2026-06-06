package com.sante.lims.controller;

import com.sante.lims.dao.BankDetailsDAO;
import com.sante.lims.dao.ResultDAO;
import com.sante.lims.dao.TestRequestDAO;
import com.sante.lims.dao.TestTypeDAO;
import com.sante.lims.model.Result;
import com.sante.lims.model.TestRequest;
import com.sante.lims.model.TestType;
import com.sante.lims.model.User;
import com.sante.lims.session.SessionManager;
import com.sante.lims.util.AuditLogger;
import com.sante.lims.util.SceneNavigator;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class CustomerDashboardController {

    @FXML private Button navBrowse, navMyOrders, navResults;
    @FXML private VBox paneBrowse, paneMyOrders, paneResults;
    @FXML private Label welcomeLabel, pageTitleLabel;
    @FXML private AnchorPane rootPane;

    // Browse
    @FXML private TableView<TestType> testsTable;
    @FXML private TableColumn<TestType, String> colBName, colBCategory, colBPrice, colBTat, colBFormat, colBOrder;
    @FXML private VBox bankDetailsBox;
    @FXML private Label bankNameLabel, bankAccNameLabel, bankAccNumLabel;

    //My Orders
    @FXML private TableView<TestRequest> ordersTable;
    @FXML private TableColumn<TestRequest, String> colOTest, colOStatus, colOPayment, colODate, colOCountdown;

    //My Results
    @FXML private TableView<TestRequest> resultsTable;
    @FXML private TableColumn<TestRequest, String> colRTest, colRFormat, colRDate, colRView, colRDown;
    @FXML private VBox resultViewBox;
    @FXML private Label resultViewTitle;
    @FXML private TextArea resultViewText;

    private final TestTypeDAO testTypeDAO   = new TestTypeDAO();
    private final TestRequestDAO requestDAO = new TestRequestDAO();
    private final ResultDAO resultDAO       = new ResultDAO();
    private final BankDetailsDAO bankDAO    = new BankDetailsDAO();

    private Timeline countdownTimeline;
    private List<TestRequest> myOrders;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML
    private void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) welcomeLabel.setText("Welcome, " + user.getFullName());

        setupBrowseTable();
        setupOrdersTable();
        setupResultsTable();
        loadTests();
        startCountdownTimer();
    }

    // Navigation

    @FXML private void onNavBrowse(ActionEvent e)   { showPane("browse"); loadTests(); }
    @FXML private void onNavMyOrders(ActionEvent e) { showPane("orders"); loadOrders(); }
    @FXML private void onNavResults(ActionEvent e)  { showPane("results"); loadResults(); }

    private void showPane(String name) {
        paneBrowse.setVisible(false);   paneBrowse.setManaged(false);
        paneMyOrders.setVisible(false); paneMyOrders.setManaged(false);
        paneResults.setVisible(false);  paneResults.setManaged(false);
        bankDetailsBox.setVisible(false); bankDetailsBox.setManaged(false);
        switch (name) {
            case "browse"  -> { paneBrowse.setVisible(true);   paneBrowse.setManaged(true);   pageTitleLabel.setText("Browse Tests"); }
            case "orders"  -> { paneMyOrders.setVisible(true); paneMyOrders.setManaged(true); pageTitleLabel.setText("My Orders"); }
            case "results" -> { paneResults.setVisible(true);  paneResults.setManaged(true);  pageTitleLabel.setText("My Results"); }
        }
    }

    // Browse & Order

    private void setupBrowseTable() {
        colBName.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getName()));
        colBCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        colBPrice.setCellValueFactory(c    -> new SimpleStringProperty("₦" + c.getValue().getPrice().toPlainString()));
        colBTat.setCellValueFactory(c      -> new SimpleStringProperty(c.getValue().getTatHours() + " hrs"));
        colBFormat.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getResultFormat().name()));
        colBOrder.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Order");
            { btn.getStyleClass().add("btn-primary"); btn.setStyle("-fx-font-size:11px;"); }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                TestType tt = getTableView().getItems().get(getIndex());
                btn.setOnAction(e -> placeOrder(tt));
                setGraphic(btn);
            }
        });
    }

    private void loadTests() {
        testsTable.setItems(FXCollections.observableArrayList(testTypeDAO.findAll()));
    }

    private void placeOrder(TestType tt) {
        User user = SessionManager.getInstance().getCurrentUser();
        requestDAO.create(user.getId(), tt.getId()).ifPresentOrElse(req -> {
            AuditLogger.log("PLACE_ORDER", "test_requests", req.getId(), "Ordered: " + tt.getName());
            // Show bank details
            bankDAO.getActive().ifPresentOrElse(bd -> {
                bankNameLabel.setText("Bank: " + bd.bankName());
                bankAccNameLabel.setText("Account Name: " + bd.accountName());
                bankAccNumLabel.setText("Account No: " + bd.accountNumber());
            }, () -> {
                bankNameLabel.setText("Bank: First Bank Nigeria");
                bankAccNameLabel.setText("Account Name: Sante Diagnostics Ltd");
                bankAccNumLabel.setText("Account No: 3012345678");
            });
            bankDetailsBox.setVisible(true); bankDetailsBox.setManaged(true);
        }, () -> showAlert("Order Failed", "Could not place your order. Please try again."));
    }

    @FXML private void onBankDetailsDone(ActionEvent e) {
        bankDetailsBox.setVisible(false); bankDetailsBox.setManaged(false);
    }

    // My Orders with countdown 

    private void setupOrdersTable() {
        colOTest.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getTestTypeName()));
        colOStatus.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getStatus().name().replace("_", " ")));
        colOPayment.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getPaymentStatus().name()));
        colODate.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt().format(FMT) : ""));
        colOCountdown.setCellValueFactory(c -> new SimpleStringProperty(formatCountdown(c.getValue())));
    }

    @FXML private void onRefreshOrders(ActionEvent e) { loadOrders(); }

    private void loadOrders() {
        User user = SessionManager.getInstance().getCurrentUser();
        myOrders = requestDAO.findByCustomer(user.getId());
        ordersTable.setItems(FXCollections.observableArrayList(myOrders));
    }

    private String formatCountdown(TestRequest req) {
        if (req.getStatus() == TestRequest.Status.COMPLETED) return "✅ Ready";
        if (req.getResultReadyAt() == null) return "Awaiting processing";
        long mins = ChronoUnit.MINUTES.between(LocalDateTime.now(), req.getResultReadyAt());
        if (mins <= 0) return "✅ Ready";
        long hours = mins / 60; long remaining = mins % 60;
        return hours + "h " + remaining + "m remaining";
    }

    private void startCountdownTimer() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(60), e -> {
            if (paneMyOrders.isVisible() && myOrders != null) {
                ordersTable.refresh();
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    // My Results

    private void setupResultsTable() {
        colRTest.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getTestTypeName()));
        colRDate.setCellValueFactory(c   -> new SimpleStringProperty(
                c.getValue().getResultReadyAt() != null ? c.getValue().getResultReadyAt().format(FMT) : ""));

        // Format column (fetched from result)
        colRFormat.setCellValueFactory(c -> {
            Optional<Result> r = resultDAO.findByRequestId(c.getValue().getId());
            return new SimpleStringProperty(r.map(res -> res.getResultFormat().name()).orElse("-"));
        });

        // View button
        colRView.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("View");
            { btn.getStyleClass().add("btn-secondary"); btn.setStyle("-fx-font-size:11px;"); }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                TestRequest req = getTableView().getItems().get(getIndex());
                btn.setOnAction(e -> viewResult(req));
                setGraphic(btn);
            }
        });

        // Download button
        colRDown.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("⬇ Save");
            { btn.getStyleClass().add("btn-primary"); btn.setStyle("-fx-font-size:11px;"); }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                TestRequest req = getTableView().getItems().get(getIndex());
                btn.setOnAction(e -> downloadResult(req));
                setGraphic(btn);
            }
        });
    }

    @FXML private void onRefreshResults(ActionEvent e) { loadResults(); }

    private void loadResults() {
        User user = SessionManager.getInstance().getCurrentUser();
        List<TestRequest> completed = requestDAO.findByCustomer(user.getId())
                .stream()
                .filter(r -> r.getStatus() == TestRequest.Status.COMPLETED)
                .toList();
        resultsTable.setItems(FXCollections.observableArrayList(completed));
        resultViewBox.setVisible(false); resultViewBox.setManaged(false);
    }

    private void viewResult(TestRequest req) {
        Optional<Result> opt = resultDAO.findByRequestId(req.getId());
        if (opt.isEmpty()) { showAlert("No Result", "No result found for this request."); return; }
        Result r = opt.get();
        switch (r.getResultFormat()) {
            case NUMERIC -> {
                resultViewTitle.setText(req.getTestTypeName() + " – Numeric Result");
                resultViewText.setText(r.getNumericValue() != null ? r.getNumericValue().toPlainString() : "N/A");
                resultViewBox.setVisible(true); resultViewBox.setManaged(true);
            }
            case TEXT -> {
                resultViewTitle.setText(req.getTestTypeName() + " – Result");
                resultViewText.setText(r.getTextValue() != null ? r.getTextValue() : "N/A");
                resultViewBox.setVisible(true); resultViewBox.setManaged(true);
            }
            case PDF, IMAGE -> {
                if (r.getFilePath() == null) { showAlert("No File", "File not available."); return; }
                try { Desktop.getDesktop().open(new File(r.getFilePath())); }
                catch (IOException ex) { showAlert("Cannot Open", "Could not open file:\n" + r.getFilePath()); }
            }
        }
    }

    private void downloadResult(TestRequest req) {
        Optional<Result> opt = resultDAO.findByRequestId(req.getId());
        if (opt.isEmpty()) { showAlert("No Result", "No result found."); return; }
        Result r = opt.get();
        if (r.getFilePath() == null) {
            //For text/numeric, write to file
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Choose Save Location");
            File dir = dc.showDialog(navBrowse.getScene().getWindow());
            if (dir == null) return;
            String content = r.getResultFormat() == Result.ResultFormat.NUMERIC
                    ? (r.getNumericValue() != null ? r.getNumericValue().toPlainString() : "N/A")
                    : (r.getTextValue() != null ? r.getTextValue() : "N/A");
            File out = new File(dir, req.getTestTypeName().replace(" ", "_") + "_result.txt");
            try { Files.writeString(out.toPath(), content); showAlert("Saved", "Saved to: " + out.getAbsolutePath()); }
            catch (IOException ex) { showAlert("Error", "Could not save file."); }
        } else {
            //to copy file to chosen directory
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Choose Save Location");
            File dir = dc.showDialog(navBrowse.getScene().getWindow());
            if (dir == null) return;
            File src = new File(r.getFilePath());
            File dest = new File(dir, src.getName());
            try { Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                  showAlert("Saved", "Saved to: " + dest.getAbsolutePath()); }
            catch (IOException ex) { showAlert("Error", "Could not copy file: " + ex.getMessage()); }
        }
    }

    //Helpers

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    @FXML private void onLogout(ActionEvent e) throws IOException {
        if (countdownTimeline != null) countdownTimeline.stop();
        AuditLogger.log("LOGOUT", "Customer logged out.");
        SessionManager.getInstance().logout();
        SceneNavigator.switchTo((Stage) navBrowse.getScene().getWindow(), "/fxml/Login.fxml", "Login");
    }
}