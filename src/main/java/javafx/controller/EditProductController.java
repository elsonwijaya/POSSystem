package javafx.controller;

import database.Database;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.model.Product;
import javafx.model.ProductType;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;

import static javafx.utils.SceneUtil.DEFAULT_WINDOW_HEIGHT;
import static javafx.utils.SceneUtil.DEFAULT_WINDOW_WIDTH;

public class EditProductController {
    @FXML
    private ComboBox<Product> productComboBox;
    @FXML
    private ComboBox<ProductType> typeComboBox;
    @FXML
    private TextField variantField;
    @FXML
    private TextField priceField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button backButton;

    private Product selectedProduct;

    @FXML
    public void initialize() {
        setupComboBoxes();
        loadProducts();
        setupProductSelectionListener();
    }

    private void setupComboBoxes() {
        // Setup product type combo box
        typeComboBox.getItems().addAll(ProductType.values());
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

        // Setup product combo box
        productComboBox.setCellFactory(lv -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s (Rp %,d)",
                            product.getType().getDisplayName(),
                            product.getVariant(),
                            (long)product.getPrice()));
                }
            }
        });

        productComboBox.setButtonCell(new ListCell<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - %s (Rp %,d)",
                            product.getType().getDisplayName(),
                            product.getVariant(),
                            (long)product.getPrice()));
                }
            }
        });
    }

    private void loadProducts() {
        List<Product> products = Database.getAllProducts();
        productComboBox.getItems().clear();
        productComboBox.getItems().addAll(products);
    }

    private void setupProductSelectionListener() {
        productComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedProduct = newVal;
                typeComboBox.setValue(newVal.getType());
                variantField.setText(newVal.getVariant());
                priceField.setText(String.format("%.0f", newVal.getPrice()));
            }
        });
    }

    @FXML
    public void handleSaveChanges() {
        if (selectedProduct == null) {
            showAlert("Error", "Please select a product to edit.");
            return;
        }

        ProductType newType = typeComboBox.getValue();
        String newVariant = variantField.getText().trim();
        String priceText = priceField.getText().trim();

        // Validation
        if (newType == null) {
            showAlert("Error", "Please select a product type.");
            return;
        }
        if (newVariant.isEmpty()) {
            showAlert("Error", "Please enter a product variant.");
            return;
        }
        if (priceText.isEmpty()) {
            showAlert("Error", "Please enter a price.");
            return;
        }

        try {
            double newPrice = Double.parseDouble(priceText);

            // Remove old product
            Database.removeProduct(selectedProduct);

            // Add updated product
            Product updatedProduct = new Product(newType, newVariant, newPrice);
            Database.addProduct(updatedProduct);

            statusLabel.setText("Product updated successfully!");
            loadProducts(); // Refresh the product list
            clearFields();
        } catch (NumberFormatException e) {
            showAlert("Error", "Price must be a number.");
        } catch (SQLException e) {
            showAlert("Error", "Error updating product: " + e.getMessage());
        }
    }

    private void clearFields() {
        productComboBox.setValue(null);
        typeComboBox.setValue(null);
        variantField.clear();
        priceField.clear();
        selectedProduct = null;
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
            showAlert("Error", "Error loading POS screen: " + e.getMessage());
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