package javafx.controller;

import database.Database;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.model.Product;
import javafx.model.ProductType;

import java.sql.SQLException;

import static javafx.utils.SceneUtil.DEFAULT_WINDOW_HEIGHT;
import static javafx.utils.SceneUtil.DEFAULT_WINDOW_WIDTH;

public class AddProductController {
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

    @FXML
    public void initialize() {
        // Populate the type combo box
        typeComboBox.getItems().addAll(ProductType.values());
        typeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProductType type) {
                return type != null ? type.getDisplayName() : "";
            }

            @Override
            public ProductType fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
    }

    @FXML
    public void handleAddProduct() {
        ProductType selectedType = typeComboBox.getValue();
        String variant = variantField.getText().trim();
        String priceText = priceField.getText().trim();

        // Validation
        if (selectedType == null) {
            statusLabel.setText("Please select a product type.");
            return;
        }
        if (variant.isEmpty()) {
            statusLabel.setText("Please enter a product variant.");
            return;
        }
        if (priceText.isEmpty()) {
            statusLabel.setText("Please enter a price.");
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            Product product = new Product(selectedType, variant, price);
            Database.addProduct(product);

            statusLabel.setText("Product added successfully!");
            clearFields();
        } catch (NumberFormatException e) {
            statusLabel.setText("Price must be a number.");
        } catch (SQLException e) {
            statusLabel.setText("Error adding product: " + e.getMessage());
        }
    }

    private void clearFields() {
        typeComboBox.setValue(null);
        variantField.clear();
        priceField.clear();
    }

    @FXML
    public void handleBack() {
        try {
            // Load the main POS screen
            Stage stage = (Stage) backButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/javafx/HomePage.fxml"));

            // Create a new Scene with CSS applied
            Scene scene = new Scene(root, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
            scene.getStylesheets().add(getClass().getResource("/css/HomePage.css").toExternalForm()); // Update the path accordingly

            stage.setScene(scene);
            stage.setTitle("POS");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error loading POS screen: " + e.getMessage());
        }
    }
}
