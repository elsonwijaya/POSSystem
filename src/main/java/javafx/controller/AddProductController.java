package javafx.controller;

import database.Database;
import javafx.fxml.FXML;
import javafx.model.Product;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.sql.SQLException;

import static javafx.utils.SceneUtil.DEFAULT_WINDOW_HEIGHT;
import static javafx.utils.SceneUtil.DEFAULT_WINDOW_WIDTH;

public class AddProductController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField priceField;

    @FXML
    private Label statusLabel;

    @FXML
    private Button backButton; // For the Back button

    @FXML
    public void handleAddProduct() {
        String name = nameField.getText().trim();
        String priceText = priceField.getText().trim();

        // Input validation
        if (name.isEmpty() || priceText.isEmpty()) {
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            Product product = new Product(name, price);
            Database.addProduct(product); // Add the product to the database

            statusLabel.setText("Product added successfully!");
            clearFields();
        } catch (NumberFormatException e) {
            statusLabel.setText("Price must be a number.");
        } catch (SQLException e) {
            statusLabel.setText("Error adding product: " + e.getMessage());
        }
    }

    private void clearFields() {
        nameField.clear();
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
