package javafx.controller;

import database.Database;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.model.Product;

import java.sql.SQLException;
import java.util.List;

import static javafx.utils.SceneUtil.DEFAULT_WINDOW_HEIGHT;
import static javafx.utils.SceneUtil.DEFAULT_WINDOW_WIDTH;

public class RemoveProductController {

    @FXML
    private ComboBox<Product> productComboBox; // Dropdown for product selection

    @FXML
    private Label statusLabel;

    @FXML
    private Button backButton;

    @FXML
    public void initialize() {
        loadProducts(); // Load products into ComboBox on initialization
    }

    private void loadProducts() {
        List<Product> products = Database.getAllProducts();
        productComboBox.getItems().clear();
        productComboBox.getItems().addAll(products);
    }

    @FXML
    private void handleRemoveProduct() {
        Product selectedProduct = productComboBox.getSelectionModel().getSelectedItem();

        // Validate that a product has been selected
        if (selectedProduct == null) {
            showAlert("Error", "Please select a product to remove.");
            return;
        }

        try {
            // Remove product using the product details, not an ID
            boolean isRemoved = Database.removeProduct(selectedProduct);
            if (isRemoved) {
                statusLabel.setText("Product removed successfully!");
                productComboBox.getItems().remove(selectedProduct); // Update ComboBox
            } else {
                statusLabel.setText("Error: Product could not be found.");
            }
        } catch (SQLException e) {
            statusLabel.setText("Error removing product: " + e.getMessage());
        }
    }

    @FXML
    public void handleBack() {
        try {
            // Load the main POS screen
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
