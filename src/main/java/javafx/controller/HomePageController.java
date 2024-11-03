package javafx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import static javafx.utils.SceneUtil.DEFAULT_WINDOW_HEIGHT;
import static javafx.utils.SceneUtil.DEFAULT_WINDOW_WIDTH;

public class HomePageController {

    @FXML
    private Button calculateTotalButton;
    @FXML
    private Button addProductButton;
    @FXML
    private Button removeProductButton;

    @FXML
    public void handleNavigateToCalculateTotal() {
        loadScene("/javafx/CalculateTotal.fxml", "Calculate Total");
    }

    @FXML
    public void handleNavigateToAddProduct() {
        loadScene("/javafx/AddProduct.fxml", "Add Product");
    }

    @FXML
    public void handleNavigateToRemoveProduct() {
        loadScene("/javafx/RemoveProduct.fxml", "Remove Product");
    }

    @FXML
    public void handleNavigateToOrderHistory() {
        loadScene("/javafx/OrderHistory.fxml", "Order History");
    }

    private void loadScene(String fxmlPath, String title) {
        try {
            Stage stage = (Stage) calculateTotalButton.getScene().getWindow(); // Use calculateTotalButton or addProductButton
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            stage.setScene(new Scene(root, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT));
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to load " + title + ": " + e.getMessage());
        }
    }
}
