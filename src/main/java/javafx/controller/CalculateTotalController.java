package javafx.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.model.ProductType;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import database.Database;
import javafx.model.Order;
import javafx.model.Product;
import java.util.stream.Collectors;
import java.util.Set;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;

import static javafx.utils.SceneUtil.DEFAULT_WINDOW_HEIGHT;
import static javafx.utils.SceneUtil.DEFAULT_WINDOW_WIDTH;

public class CalculateTotalController {

    @FXML
    private ComboBox<ProductType> typeComboBox;
    @FXML
    private ComboBox<Product> variantComboBox;
    @FXML
    private TextField quantityTextField;
    @FXML
    private Label totalLabel;
    @FXML
    private Label changeLabel;
    @FXML
    private Button checkoutButton;
    @FXML
    private Button backButton;

    private Order currentOrder;
    private double lastCashGiven = 0.0;
    private double lastChange = 0.0;
    private List<Product> allProducts;

    public void initialize() {
        currentOrder = new Order();
        loadProducts();
        setupComboBoxes();
        updateCheckoutButton();
    }

    private void loadProducts() {
        allProducts = Database.getAllProducts();

        // Populate type ComboBox
        Set<ProductType> types = allProducts.stream()
                .map(Product::getType)
                .collect(Collectors.toSet());
        typeComboBox.getItems().addAll(types);

        // Setup type ComboBox display
        typeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProductType type) {
                return type != null ? type.getDisplayName() : "";
            }

            @Override
            public ProductType fromString(String string) {
                return null;
            }
        });
    }

    private void setupComboBoxes() {
        // When type is selected, update variant ComboBox
        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                List<Product> variants = allProducts.stream()
                        .filter(p -> p.getType() == newVal)
                        .collect(Collectors.toList());
                variantComboBox.getItems().clear();
                variantComboBox.getItems().addAll(variants);
            }
        });

        // Setup variant ComboBox display
        variantComboBox.setCellFactory(lv -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setText(null);
                } else {
                    setText(String.format("%s (Rp %,d)",
                            product.getVariant(),
                            (long)product.getPrice()));
                }
            }
        });

        variantComboBox.setButtonCell(new ListCell<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setText(null);
                } else {
                    setText(String.format("%s (Rp %,d)",
                            product.getVariant(),
                            (long)product.getPrice()));
                }
            }
        });
    }

    @FXML
    private void handleAddButton() {
        Product selectedProduct = variantComboBox.getSelectionModel().getSelectedItem();
        String quantityStr = quantityTextField.getText();

        if (!validateInput(selectedProduct, quantityStr)) {
            return;
        }

        int quantity = Integer.parseInt(quantityStr);
        currentOrder.addItem(selectedProduct, quantity);
        updateTotalDisplay();
        updateCheckoutButton();

        // Clear inputs after adding
        typeComboBox.getSelectionModel().clearSelection();
        variantComboBox.getItems().clear();
        quantityTextField.clear();
    }

    private void updateTotalDisplay() {
        totalLabel.setText(String.format("Total: Rp %,d", (long)currentOrder.getTotal()));
        // If we have previous cash/change information, update that display too
        if (lastCashGiven > 0) {
            changeLabel.setVisible(true);
            changeLabel.setText(String.format("Cash: Rp %,d | Change: Rp %,d",
                    (long)lastCashGiven, (long)lastChange));
        }
    }

    private boolean validateInput(Product selectedProduct, String quantityStr) {
        if (typeComboBox.getValue() == null) {
            showAlert("Error", "Please select a product type.");
            return false;
        }
        if (selectedProduct == null) {
            showAlert("Error", "Please select a product variant.");
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

        // Create a custom dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Payment Method");
        dialog.setHeaderText("Select payment method or enter cash amount:");

        // Create a layout for the dialog content
        VBox layout = new VBox(10);  // 10 pixels spacing
        layout.setPadding(new Insets(10));

        // Add cash amount input field
        TextField cashInput = new TextField();
        cashInput.setPromptText("Enter cash amount");
        layout.getChildren().add(new Label("Cash Amount:"));
        layout.getChildren().add(cashInput);

        // Add "Correct Amount" button
        Button correctAmountBtn = new Button("Exact Amount");
        correctAmountBtn.setMaxWidth(Double.MAX_VALUE); // Make button full width
        correctAmountBtn.setOnAction(e -> {
            cashInput.setText(String.format("%.0f", currentOrder.getTotal()));
        });
        layout.getChildren().add(correctAmountBtn);

        // Add "E-Payment" button
        Button ePaymentBtn = new Button("E-Payment");
        ePaymentBtn.setMaxWidth(Double.MAX_VALUE); // Make button full width
        ePaymentBtn.setOnAction(e -> {
            cashInput.setText(String.format("%.0f", currentOrder.getTotal()));
            dialog.setResult("epayment");
            dialog.close();
        });
        layout.getChildren().add(ePaymentBtn);

        // Set the custom layout to the dialog
        dialog.getDialogPane().setContent(layout);

        // Add OK and Cancel buttons
        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // Enable/Disable confirm button depending on whether a cash amount was entered
        Node confirmButton = dialog.getDialogPane().lookupButton(confirmButtonType);
        cashInput.textProperty().addListener((observable, oldValue, newValue) -> {
            confirmButton.setDisable(newValue.trim().isEmpty());
        });

        // Set the result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return cashInput.getText();
            }
            return null;
        });

        // Show dialog and process result
        dialog.showAndWait().ifPresent(result -> {
            try {
                if ("epayment".equals(result)) {
                    // Handle E-Payment case
                    processCheckout(currentOrder.getTotal(), true);
                } else {
                    // Handle cash payment case
                    String cleanValue = result.replace(",", "");
                    double cashGiven = Double.parseDouble(cleanValue);
                    processCheckout(cashGiven, false);
                }
            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid input. Please enter a valid number.");
            }
        });
    }

    private void processCheckout(double cashGiven, boolean isEPayment) {  // Added isEPayment parameter
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
        changeLabel.setText(String.format("%s: Rp %,d | Change: Rp %,d",
                isEPayment ? "E-payment" : "Cash",
                (long)cashGiven, (long)change));

        // Show proceed to receipt confirmation
        Alert confirmReceipt = new Alert(Alert.AlertType.CONFIRMATION);
        confirmReceipt.setTitle("Proceed to Receipt");
        confirmReceipt.setHeaderText("Transaction Complete");
        confirmReceipt.setContentText(String.format(
                "Total: Rp %,d\n%s: Rp %,d\nChange: Rp %,d\n\nWould you like to view the receipt?",
                (long)total,
                isEPayment ? "E-payment" : "Cash",
                (long)cashGiven, (long)change));

        confirmReceipt.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Save the order to database before showing receipt
                    Database.saveOrder(currentOrder);

                    // After successful save, show the receipt
                    loadReceiptView(cashGiven, change, isEPayment);
                } catch (SQLException e) {
                    showAlert("Error", "Failed to save order: " + e.getMessage());
                    // Still show receipt even if save fails
                    loadReceiptView(cashGiven, change, isEPayment);
                }
            }
        });
    }

    private void loadReceiptView(double cashGiven, double change, boolean isEPayment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx/Receipt.fxml"));
            Parent root = loader.load();
            ReceiptController receiptController = loader.getController();
            receiptController.setOrder(currentOrder);
            receiptController.setCashDetails(cashGiven, change, isEPayment);

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