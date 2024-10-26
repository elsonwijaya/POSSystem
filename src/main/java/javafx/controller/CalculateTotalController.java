package javafx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import database.Database;
import javafx.model.Order;
import javafx.model.Product;

import java.util.List;

import static javafx.utils.SceneUtil.DEFAULT_WINDOW_HEIGHT;
import static javafx.utils.SceneUtil.DEFAULT_WINDOW_WIDTH;

public class CalculateTotalController {

    @FXML
    private ComboBox<Product> productComboBox;
    @FXML
    private TextField quantityTextField;
    @FXML
    private Label totalLabel;
    @FXML
    private Label changeLabel;
    @FXML
    private Button backButton;
    @FXML
    private Button checkoutButton;

    private Order currentOrder;
    private double lastCashGiven = 0.0;
    private double lastChange = 0.0;

    public void initialize() {
        currentOrder = new Order();
        loadProducts();
        updateCheckoutButton();
    }

    private void loadProducts() {
        List<Product> products = Database.getAllProducts();
        productComboBox.getItems().clear();
        productComboBox.getItems().addAll(products);
    }

    @FXML
    private void handleAddButton() {
        Product selectedProduct = productComboBox.getSelectionModel().getSelectedItem();
        String quantityStr = quantityTextField.getText();

        if (!validateInput(selectedProduct, quantityStr)) {
            return;
        }

        int quantity = Integer.parseInt(quantityStr);
        currentOrder.addItem(selectedProduct, quantity);
        updateTotalDisplay();
        updateCheckoutButton();

        // Clear inputs after adding
        productComboBox.getSelectionModel().clearSelection();
        quantityTextField.clear();
    }

    private void updateTotalDisplay() {
        totalLabel.setText(String.format("Total: Rp%.2f", currentOrder.getTotal()));
        // If we have previous cash/change information, update that display too
        if (lastCashGiven > 0) {
            changeLabel.setVisible(true);
            changeLabel.setText(String.format("Cash: Rp%.2f | Change: Rp%.2f",
                    lastCashGiven, lastChange));
        }
    }

    private boolean validateInput(Product selectedProduct, String quantityStr) {
        if (selectedProduct == null) {
            showAlert("Error", "Please select a product.");
            return false;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                showAlert("Error", "Quantity must be greater than zero.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid quantity. Please enter a valid number.");
            return false;
        }

        return true;
    }

    @FXML
    private void handleCheckout() {
        if (currentOrder.getItems().isEmpty()) {
            showAlert("Error", "Cannot checkout, no items in the order.");
            return;
        }

        TextInputDialog cashDialog = new TextInputDialog();
        cashDialog.setTitle("Cash Given");
        cashDialog.setHeaderText("Enter cash given by the customer:");
        cashDialog.setContentText("Cash Amount:");

        cashDialog.showAndWait().ifPresent(cashGivenStr -> {
            try {
                double cashGiven = Double.parseDouble(cashGivenStr);
                processCheckout(cashGiven);
            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid input. Please enter a valid number.");
            }
        });
    }

    private void processCheckout(double cashGiven) {
        double total = currentOrder.getTotal();
        double change = cashGiven - total;

        if (change < 0) {
            showAlert("Error", "Not enough cash given!");
            return;
        }

        // Store the values
        this.lastCashGiven = cashGiven;
        this.lastChange = change;

        // Update the display immediately
        changeLabel.setVisible(true);
        changeLabel.setText(String.format("Cash: Rp%.2f | Change: Rp%.2f",
                cashGiven, change));

        // Show proceed to receipt confirmation
        Alert confirmReceipt = new Alert(Alert.AlertType.CONFIRMATION);
        confirmReceipt.setTitle("Proceed to Receipt");
        confirmReceipt.setHeaderText("Transaction Complete");
        confirmReceipt.setContentText(String.format(
                "Total: Rp%.2f\nCash: Rp%.2f\nChange: Rp%.2f\n\nWould you like to view the receipt?",
                total, cashGiven, change));

        confirmReceipt.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                loadReceiptView(cashGiven, change);
            }
        });
    }

    private void loadReceiptView(double cashGiven, double change) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx/Receipt.fxml"));
            Parent root = loader.load();
            ReceiptController receiptController = loader.getController();
            receiptController.setOrder(currentOrder);
            receiptController.setCashDetails(cashGiven, change);

            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
            stage.setScene(scene);
            stage.setTitle("Receipt");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error loading receipt: " + e.getMessage());
        }
    }

    @FXML
    public void handleBack() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/javafx/HomePage.fxml"));
            Scene scene = new Scene(root, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
            scene.getStylesheets().add(getClass().getResource("/css/HomePage.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("POS");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error loading POS screen: " + e.getMessage());
        }
    }

    private void updateCheckoutButton() {
        checkoutButton.setDisable(currentOrder.getItems().isEmpty());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}