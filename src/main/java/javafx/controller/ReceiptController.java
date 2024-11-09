package javafx.controller;

import database.Database;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.model.Receipt;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.model.Order;
import javafx.utils.ThermalPrinter;

import java.io.File;
import static javafx.utils.SceneUtil.DEFAULT_WINDOW_HEIGHT;
import static javafx.utils.SceneUtil.DEFAULT_WINDOW_WIDTH;

public class ReceiptController {
    @FXML
    private Label receiptLabel;
    @FXML
    private Button backButton;

    private Order order;
    private double cashGiven;
    private double change;
    private boolean isEPayment;

    public void setOrder(Order order) {
        this.order = order;
        if (order != null) {
            generateAndUpdateReceipt();
        }
    }

    public void setCashDetails(double cashGiven, double change, boolean isEPayment) {
        this.cashGiven = cashGiven;
        this.change = change;
        this.isEPayment = isEPayment;
        generateAndUpdateReceipt();
    }

    private void generateAndUpdateReceipt() {
        if (order == null) return;

        StringBuilder receiptText = new StringBuilder("Receipt:\n\nItems:\n");
        order.getItems().forEach((product, quantity) -> {
            receiptText.append(String.format("%s - %s\n",
                    product.getType().getDisplayName(),
                    product.getVariant()));
            receiptText.append(String.format("Rp %,d\n",
                    (long)(product.getPrice() * quantity)));
        });

        receiptText.append("\nTotal: Rp ").append(String.format("%,d", (long)order.getTotal())).append("\n");

        // Use E-payment or Cash based on payment method
        if (isEPayment) {
            receiptText.append("E-payment: Rp ").append(String.format("%,d", (long)cashGiven)).append("\n");
        } else {
            receiptText.append("Cash: Rp ").append(String.format("%,d", (long)cashGiven)).append("\n");
        }

        receiptText.append("Change: Rp ").append(String.format("%,d", (long)change)).append("\n");

        receiptLabel.setText(receiptText.toString());
    }

    @FXML
    public void handlePrint() {
        try {
            Receipt receipt = new Receipt(order, cashGiven, change, isEPayment);
            ThermalPrinter printer = new ThermalPrinter(receipt);

            // Save PDF copy
            String fileName = Database.generateReceiptFileName("thermal");
            String receiptPath = Database.getAppDirectory();
            File receiptDir = new File(receiptPath);
            if (!receiptDir.exists()) {
                receiptDir.mkdirs();
            }

            String fullPath = receiptPath + File.separator + fileName;
            printer.printReceipt(receipt, fullPath);

            // Print to thermal printer
            printer.printToThermalPrinter();

            showSuccess("Receipt printed successfully and PDF copy saved!");
            handleBack();
        } catch (Exception e) {
            showError("Error printing receipt: " + e.getMessage());
        }
    }

    @FXML
    public void handleBack() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/javafx/CalculateTotal.fxml"));
            Scene scene = new Scene(root, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
            scene.getStylesheets().add(getClass().getResource("/css/HomePage.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("POS");
            stage.show();
        } catch (Exception e) {
            showError("Error loading POS screen: " + e.getMessage());
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}