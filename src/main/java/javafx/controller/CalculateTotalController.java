package javafx.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.model.CartItem;
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

import java.io.IOException;
import java.util.ArrayList;
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
    private Label discountLabel;
    @FXML
    private Label finalTotalLabel;
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
        double subtotal = currentOrder.getSubtotal();
        double discount = currentOrder.getDiscount();
        double finalTotal = currentOrder.getTotal();

        // Update labels
        totalLabel.setText(String.format("Subtotal: Rp %,d", (long)subtotal));

        if (discount > 0) {
            discountLabel.setVisible(true);
            discountLabel.setText(String.format("%s\nDiscount: -Rp %,d",
                    currentOrder.getDiscountDescription(),
                    (long)discount));
        } else {
            discountLabel.setVisible(false);
        }

        finalTotalLabel.setText(String.format("Final Total: Rp %,d", (long)finalTotal));

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

    private void processCheckout(double cashGiven, boolean isEPayment) {
        double finalTotal = currentOrder.getTotal(); // This is the discounted total
        double change = cashGiven - finalTotal;

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

        StringBuilder contentText = new StringBuilder();
        contentText.append(String.format("Subtotal: Rp %,d\n", (long)currentOrder.getSubtotal()));

        if (currentOrder.getDiscount() > 0) {
            contentText.append(String.format("%s\n", currentOrder.getDiscountDescription()));
            contentText.append(String.format("Discount: -Rp %,d\n", (long)currentOrder.getDiscount()));
        }

        contentText.append(String.format("Final Total: Rp %,d\n", (long)finalTotal));
        contentText.append(String.format("%s: Rp %,d\n", isEPayment ? "E-payment" : "Cash", (long)cashGiven));
        contentText.append(String.format("Change: Rp %,d\n\n", (long)change));
        contentText.append("Would you like to view the receipt?");

        confirmReceipt.setContentText(contentText.toString());

        confirmReceipt.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    Database.saveOrder(currentOrder);
                    loadReceiptView(cashGiven, change, isEPayment);
                } catch (SQLException e) {
                    showAlert("Error", "Failed to save order: " + e.getMessage());
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

    @FXML
    private void handleViewCart() {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("View Cart");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafx/ViewCart.fxml"));
            DialogPane dialogPane = loader.load();
            dialog.setDialogPane(dialogPane);

            TableView<CartItem> cartTable = (TableView<CartItem>) dialogPane.lookup("#cartTable");
            setupCartColumns(cartTable);

            // Convert current order items to observable list
            List<CartItem> cartItems = new ArrayList<>();
            currentOrder.getItems().forEach((product, quantity) ->
                    cartItems.add(new CartItem(product, quantity)));

            // Use ObservableList instead of regular List
            var observableItems = FXCollections.observableArrayList(cartItems);
            cartTable.setItems(observableItems);

            Button removeButton = (Button) dialogPane.lookup("#removeButton");
            removeButton.setOnAction(event -> {
                CartItem selectedItem = cartTable.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    currentOrder.getItems().remove(selectedItem.getProduct());
                    observableItems.remove(selectedItem);  // This will update the display immediately
                    updateTotalDisplay();
                    updateCheckoutButton();
                }
            });

            dialog.showAndWait();

        } catch (IOException e) {
            showAlert("Error", "Failed to open cart view: " + e.getMessage());
        }
    }

    // Helper method to setup cart columns
    private void setupCartColumns(TableView<CartItem> cartTable) {
        TableColumn<CartItem, String> typeColumn = (TableColumn<CartItem, String>) cartTable.getColumns().get(0);
        TableColumn<CartItem, String> variantColumn = (TableColumn<CartItem, String>) cartTable.getColumns().get(1);
        TableColumn<CartItem, Integer> quantityColumn = (TableColumn<CartItem, Integer>) cartTable.getColumns().get(2);
        TableColumn<CartItem, String> priceColumn = (TableColumn<CartItem, String>) cartTable.getColumns().get(3);
        TableColumn<CartItem, String> totalColumn = (TableColumn<CartItem, String>) cartTable.getColumns().get(4);

        typeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProduct().getType().getDisplayName()));
        variantColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getProduct().getVariant()));
        quantityColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        priceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("Rp %,d",
                        (long)data.getValue().getProduct().getPrice())));
        totalColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("Rp %,d",
                        (long)(data.getValue().getProduct().getPrice() * data.getValue().getQuantity()))));
    }
}