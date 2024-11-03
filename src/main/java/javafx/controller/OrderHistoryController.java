package javafx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import database.Database;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleStringProperty;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static javafx.utils.SceneUtil.DEFAULT_WINDOW_HEIGHT;
import static javafx.utils.SceneUtil.DEFAULT_WINDOW_WIDTH;

public class OrderHistoryController {

    @FXML private TableView<Map<String, Object>> orderTable;
    @FXML private TableColumn<Map<String, Object>, String> orderIdColumn;
    @FXML private TableColumn<Map<String, Object>, String> dateColumn;
    @FXML private TableColumn<Map<String, Object>, String> totalColumn;

    @FXML private VBox orderDetailsPane;
    @FXML private TableView<Map<String, Object>> orderItemsTable;
    @FXML private TableColumn<Map<String, Object>, String> productColumn;
    @FXML private TableColumn<Map<String, Object>, String> quantityColumn;
    @FXML private TableColumn<Map<String, Object>, String> priceColumn;

    @FXML private Button backButton;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        setupColumns();
        loadOrderHistory();

        // Show order details when an order is selected
        orderTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        showOrderDetails(newSelection);
                    }
                });
    }

    private void setupColumns() {
        orderIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().get("orderId").toString()));

        dateColumn.setCellValueFactory(data -> {
            Timestamp ts = (Timestamp) data.getValue().get("date");
            return new SimpleStringProperty(ts.toLocalDateTime().format(DATE_FORMATTER));
        });

        totalColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("Rp %,d",
                        (long)((Double)data.getValue().get("total")).doubleValue())));

        productColumn.setCellValueFactory(data ->
                new SimpleStringProperty((String)data.getValue().get("productName")));

        quantityColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().get("quantity").toString()));

        priceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("Rp %,d",
                        (long)((Double)data.getValue().get("pricePerUnit")).doubleValue())));
    }

    private void loadOrderHistory() {
        try {
            List<Map<String, Object>> orders = Database.getOrderHistory();
            orderTable.setItems(FXCollections.observableArrayList(orders));
        } catch (Exception e) {
            showAlert("Error", "Failed to load order history: " + e.getMessage());
        }
    }

    private void showOrderDetails(Map<String, Object> order) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
        orderItemsTable.setItems(FXCollections.observableArrayList(items));
        orderDetailsPane.setVisible(true);
    }

    @FXML
    public void handleBack() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/javafx/HomePage.fxml"));
            Scene scene = new Scene(root, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
            scene.getStylesheets().add(getClass().getResource("/css/HomePage.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Error returning to home page: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}