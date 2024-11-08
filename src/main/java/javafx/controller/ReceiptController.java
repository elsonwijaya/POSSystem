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
import java.util.Optional;
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

    public void setOrder(Order order) {
        this.order = order;
        if (order != null) {
            generateAndUpdateReceipt();
        }
    }

    public void setCashDetails(double cashGiven, double change) {
        this.cashGiven = cashGiven;
        this.change = change;
        generateAndUpdateReceipt();
    }

    private void generateAndUpdateReceipt() {
        if (order == null) return;

        StringBuilder receiptText = new StringBuilder("Receipt:\n\nItems:\n");
        order.getItems().forEach((product, quantity) -> {
            receiptText.append(String.format("%s (%d pax)\n", product.getName(), quantity))
                    .append(String.format("%dx%,d = Rp %,d\n",
                            quantity,
                            (long)product.getPrice(),
                            (long)product.getPrice() * quantity));
        });

        receiptText.append("\nTotal: Rp ").append(String.format("%,d", (long)order.getTotal())).append("\n")
                .append("Cash: Rp ").append(String.format("%,d", (long)cashGiven)).append("\n")
                .append("Change: Rp ").append(String.format("%,d", (long)change)).append("\n");

        receiptLabel.setText(receiptText.toString());
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

    @FXML
    public void handlePrint() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Print Options");
        alert.setHeaderText("Select Print Method");
        alert.setContentText("Choose your preferred printing method:");

        ButtonType thermalButton = new ButtonType("Thermal Printer");
        ButtonType pdfButton = new ButtonType("Save as PDF");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(thermalButton, pdfButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == thermalButton) {
                printThermal();
            } else if (result.get() == pdfButton) {
                saveThermalAsPDF();
            }
        }
    }

    private void printThermal() {
        try {
            Receipt receipt = new Receipt(order, cashGiven, change);
            ThermalPrinter printer = new ThermalPrinter(receipt);
            printer.printToThermalPrinter();
            showSuccess("Receipt printed successfully!");
            handleBack();
        } catch (Exception e) {
            showError("Error printing to thermal printer: " + e.getMessage());
        }
    }

    private void saveThermalAsPDF() {
        try {
            Receipt receipt = new Receipt(order, cashGiven, change);
            ThermalPrinter printer = new ThermalPrinter(receipt);

            String fileName = Database.generateReceiptFileName("thermal");
            String receiptPath = Database.getAppDirectory();
            File receiptDir = new File(receiptPath);
            if (!receiptDir.exists()) {
                receiptDir.mkdirs();
            }

            String fullPath = receiptPath + File.separator + fileName;
            printer.printReceipt(receipt, fullPath);

            showSuccess("Receipt saved as PDF successfully!\nLocation: " + fullPath);
            handleBack();
        } catch (Exception e) {
            showError("Error saving PDF: " + e.getMessage());
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