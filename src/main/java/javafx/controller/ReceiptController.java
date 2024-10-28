package javafx.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.model.Product;
import javafx.model.Receipt;

import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.model.Order;
import javafx.utils.ThermalPrinter;

import java.io.File;
import java.util.Optional;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import static javafx.utils.SceneUtil.DEFAULT_WINDOW_HEIGHT;
import static javafx.utils.SceneUtil.DEFAULT_WINDOW_WIDTH;

public class ReceiptController {

    @FXML
    private Label receiptLabel; // Label to display the receipt
    @FXML
    private Button backButton; // Reference to the Back button

    private Order order;
    private double cashGiven;
    private double change;

    // Setter for the order
    public void setOrder(Order order) {
        this.order = order;
        if (order != null) {
            generateAndUpdateReceipt(); // Generate receipt when order is set
        }
    }

    // Setter for cash details
    public void setCashDetails(double cashGiven, double change) {
        this.cashGiven = cashGiven;
        this.change = change;
        generateAndUpdateReceipt();
    }

    private void generateAndUpdateReceipt() {
        if (order == null) return;

        StringBuilder receiptText = new StringBuilder("Receipt:\n\nItems:\n");
        order.getItems().forEach((product, quantity) -> {
            receiptText.append(product.getName())
                    .append(" x").append(quantity)
                    .append(" - Rp").append(product.getPrice() * quantity).append("\n");
        });

        receiptText.append("\nTotal: Rp").append(order.getTotal()).append("\n")
                .append("Cash Given: Rp").append(cashGiven).append("\n")
                .append("Change: Rp").append(change).append("\n");

        receiptLabel.setText(receiptText.toString());
    }

    @FXML
    public void handleBack() {
        try {
            // Load the main POS screen
            Stage stage = (Stage) backButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/javafx/CalculateTotal.fxml"));

            // Create a new Scene with CSS applied
            Scene scene = new Scene(root, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
            scene.getStylesheets().add(getClass().getResource("/css/HomePage.css").toExternalForm()); // Update the path accordingly

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
        alert.setHeaderText("Select Printer Type");
        alert.setContentText("Choose your preferred printing method:");

        ButtonType thermalButton = new ButtonType("Thermal Printer");
        ButtonType pdfButton = new ButtonType("Save as PDF");
        ButtonType regularButton = new ButtonType("Regular Printer");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(thermalButton, pdfButton, regularButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == thermalButton) {
                printThermal();
            } else if (result.get() == pdfButton) {
                saveThermalAsPDF();
            } else if (result.get() == regularButton) {
                printRegular();
            }
        }
    }

    private void printThermal() {
        try {
            Receipt receipt = new Receipt(order, cashGiven, change);
            ThermalPrinter printer = new ThermalPrinter();

            // Create a temporary file path for the thermal printer output
            String outputPath = System.getProperty("java.io.tmpdir") + "/receipt_" +
                    System.currentTimeMillis() + ".pdf";

            printer.printReceipt(receipt, outputPath);
            showSuccess("Receipt printed successfully!");

            handleBack();
        } catch (Exception e) {
            showError("Error printing to thermal printer: " + e.getMessage());
        }
    }

    private void saveThermalAsPDF() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Receipt PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            fileChooser.setInitialFileName("receipt.pdf");

            Stage stage = (Stage) receiptLabel.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                Receipt receipt = new Receipt(order, cashGiven, change);
                ThermalPrinter printer = new ThermalPrinter();
                printer.printReceipt(receipt, file.getAbsolutePath());
                showSuccess("Receipt saved as PDF successfully!");

                handleBack();
            }
        } catch (Exception e) {
            showError("Error saving PDF: " + e.getMessage());
        }
    }

    private void printRegular() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(receiptLabel.getScene().getWindow())) {
            boolean success = job.printPage(createPrintNode());
            if (success) {
                job.endJob();
                showSuccess("Receipt printed successfully!");
                handleBack();
            } else {
                showError("Failed to print receipt.");
            }
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

    private Node createPrintNode() {
        VBox printLayout = new VBox();
        printLayout.setAlignment(Pos.CENTER);
        printLayout.setSpacing(10);

        Receipt receipt = new Receipt(order, cashGiven, change);

        // Center-align Business Name and Slogan labels
        Label businessNameLabel = new Label(receipt.getBusinessName());
        businessNameLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 20px; -fx-font-weight: bold;");
        businessNameLabel.setAlignment(Pos.CENTER); // Center alignment for business name

        Label sloganLabel = new Label(receipt.getSlogan());
        sloganLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 8px; -fx-font-style: italic;");
        sloganLabel.setAlignment(Pos.CENTER); // Center alignment for slogan

        printLayout.getChildren().addAll(businessNameLabel, sloganLabel);

        // Dynamic Separator
        Region separator1 = new Region();
        separator1.setMinHeight(1);
        separator1.setStyle("-fx-background-color: black;");
        printLayout.getChildren().add(separator1);

        // Invoice Title
        Label invoiceTitleLabel = new Label("INVOICE");
        invoiceTitleLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 18px; -fx-font-weight: bold;");
        printLayout.getChildren().add(invoiceTitleLabel);

        // Dynamic Separator
        Region separator2 = new Region();
        separator2.setMinHeight(1);
        separator2.setStyle("-fx-background-color: black;");
        printLayout.getChildren().add(separator2);

        // Header for Description and Amount
        HBox headerLayout = new HBox();
        headerLayout.setAlignment(Pos.CENTER_LEFT);
        headerLayout.setSpacing(30); // Add spacing for better separation

        // Description Header
        Label descriptionHeaderLabel = new Label("Description");
        descriptionHeaderLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold;");

        // Amount Header
        Label amountHeaderLabel = new Label("Amount");
        amountHeaderLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold;");

        // Add flexible space using Region to center Amount header
        Region amountSpacer = new Region();
        HBox.setHgrow(amountSpacer, Priority.ALWAYS); // Allow the spacer to grow and take available space

        // Add elements to the header layout
        headerLayout.getChildren().addAll(descriptionHeaderLabel, amountSpacer, amountHeaderLabel);
        printLayout.getChildren().add(headerLayout);

        // Dynamic Separator
        Region separator3 = new Region();
        separator3.setMinHeight(1);
        separator3.setStyle("-fx-background-color: black;");
        printLayout.getChildren().add(separator3);

        // Order Details
        for (Map.Entry<Product, Integer> entry : order.getItems().entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            double totalPrice = product.getPrice() * quantity;

            // Product name left-aligned
            Label productNameLabel = new Label(String.format("%-30s", product.getName()));
            productNameLabel.setStyle("-fx-font-family: 'Helvetica';");

            // Price and quantity breakdown
            String amountText = String.format("%dxRp %.2f = Rp %.2f", quantity, product.getPrice(), totalPrice);
            Label productAmountLabel = new Label(amountText);
            productAmountLabel.setStyle("-fx-font-family: 'Helvetica';");

            // Create a new HBox for item layout
            HBox itemLayout = new HBox();
            itemLayout.setAlignment(Pos.CENTER_RIGHT); // Align contents to the right
            HBox.setHgrow(productNameLabel, Priority.ALWAYS); // Allow dynamic width for the product name

            // Add flexible space to push amountLabel to the right
            Region itemSpacer = new Region();
            HBox.setHgrow(itemSpacer, Priority.ALWAYS);

            // Add elements to item layout
            itemLayout.getChildren().addAll(productNameLabel, itemSpacer, productAmountLabel);

            printLayout.getChildren().add(itemLayout);
        }

        // Dynamic Separator
        Region separator4 = new Region();
        separator4.setMinHeight(1);
        separator4.setStyle("-fx-background-color: black;");
        printLayout.getChildren().add(separator4);

        // Financial Summary
        HBox totalLayout = new HBox();
        totalLayout.setAlignment(Pos.CENTER_RIGHT);
        Label totalLabel = new Label(String.format("Total: Rp %.2f", order.getTotal()));
        totalLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold;");
        totalLayout.getChildren().add(totalLabel);
        printLayout.getChildren().add(totalLayout);

        HBox amountLayout = new HBox();
        amountLayout.setAlignment(Pos.CENTER_RIGHT);
        Label amountPaidLabel = new Label(String.format("Amount Paid: Rp %.2f", cashGiven));
        amountPaidLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold;");
        amountLayout.getChildren().add(amountPaidLabel);
        printLayout.getChildren().add(amountLayout);

        HBox changeLayout = new HBox();
        changeLayout.setAlignment(Pos.CENTER_RIGHT);
        Label changeLabel = new Label(String.format("Change: Rp %.2f", change));
        changeLabel.setStyle("-fx-font-family: 'Helvetica';");
        changeLayout.getChildren().add(changeLabel);
        printLayout.getChildren().add(changeLayout);

        // Dynamic Separator
        Region separator5 = new Region();
        separator5.setMinHeight(1);
        separator5.setStyle("-fx-background-color: black;");
        printLayout.getChildren().add(separator5);

        // Dynamic Separator
        Region separator6 = new Region();
        separator6.setMinHeight(1);
        separator6.setStyle("-fx-background-color: black;");
        printLayout.getChildren().add(separator6);

        // Date and Timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String dateTime = java.time.LocalDateTime.now().format(formatter);

        HBox dateLayout = new HBox();
        dateLayout.setAlignment(Pos.CENTER_LEFT);
        Label dateTimeLabel = new Label("Date: " + dateTime);
        dateTimeLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 12px;");
        dateLayout.getChildren().add(dateTimeLabel);
        printLayout.getChildren().add(dateLayout);

        // Extra separator line after the date
        printLayout.getChildren().add(new Label("")); // Optional empty line

        // Thank you message (centered)
        Label thankYouLabel = new Label("Thank you for your purchase!");
        thankYouLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 12px;");
        printLayout.getChildren().add(thankYouLabel);

        // Instagram and Phone information (left-aligned with spaces)
        Label instagramLabel = new Label("Instagram: " + receipt.getInstagram());
        instagramLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 12px;");

        Label phoneLabel = new Label("Phone: " + receipt.getPhone());
        phoneLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 12px;");

        // Add labels to print layout
        printLayout.getChildren().addAll(instagramLabel, phoneLabel);

        // Extra line after the thank-you message
        printLayout.getChildren().add(new Label("")); // Optional empty line

        return printLayout;
    }



    /*
    private void printReceipt() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(receiptLabel.getScene().getWindow())) {
            boolean success = job.printPage(createPrintNode());
            if (success) {
                job.endJob();
                receiptLabel.setText("Receipt printed successfully!");
            } else {
                showError("Failed to print receipt.");
            }
        } else {
            showError("Print job was canceled.");
        }
    }
    */

    /*
    private Node createPrintNode() {
        VBox printLayout = new VBox();
        printLayout.setAlignment(Pos.CENTER);
        printLayout.setSpacing(10);

        // Business Name and Slogan
        Label businessNameLabel = new Label("businessname");
        businessNameLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label sloganLabel = new Label("slogan");
        sloganLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 8px; -fx-font-style: italic;");
        printLayout.getChildren().addAll(businessNameLabel, sloganLabel);

        // Dynamic Separator
        Region separator1 = new Region();
        separator1.setMinHeight(1);
        separator1.setStyle("-fx-background-color: black;");
        printLayout.getChildren().add(separator1);

        // Invoice Title
        Label invoiceTitleLabel = new Label("INVOICE");
        invoiceTitleLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 18px; -fx-font-weight: bold;");
        printLayout.getChildren().add(invoiceTitleLabel);

        // Dynamic Separator
        Region separator2 = new Region();
        separator2.setMinHeight(1);
        separator2.setStyle("-fx-background-color: black;");
        printLayout.getChildren().add(separator2);

        // Header for Description and Amount
        HBox headerLayout = new HBox();
        headerLayout.setAlignment(Pos.CENTER_LEFT);
        headerLayout.setSpacing(30); // Add spacing for better separation

        // Description Header
        Label descriptionHeaderLabel = new Label("Description");
        descriptionHeaderLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold;");

        // Amount Header
        Label amountHeaderLabel = new Label("Amount");
        amountHeaderLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold;");

        // Add flexible space using Region to center Amount header
        Region amountSpacer = new Region();
        HBox.setHgrow(amountSpacer, Priority.ALWAYS); // Allow the spacer to grow and take available space

        // Add elements to the header layout
        headerLayout.getChildren().addAll(descriptionHeaderLabel, amountSpacer, amountHeaderLabel);
        printLayout.getChildren().add(headerLayout);

        // Dynamic Separator
        Region separator3 = new Region();
        separator3.setMinHeight(1);
        separator3.setStyle("-fx-background-color: black;");
        printLayout.getChildren().add(separator3);

        // Order Details
        for (Map.Entry<Product, Integer> entry : order.getItems().entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            double totalPrice = product.getPrice() * quantity;

            // Product name left-aligned
            Label productNameLabel = new Label(String.format("%-30s", product.getName()));
            productNameLabel.setStyle("-fx-font-family: 'Helvetica';");

            // Price and quantity breakdown
            String amountText = String.format("%dxRp %.2f = Rp %.2f", quantity, product.getPrice(), totalPrice);
            Label productAmountLabel = new Label(amountText);
            productAmountLabel.setStyle("-fx-font-family: 'Helvetica';");

            // Create a new HBox for item layout
            HBox itemLayout = new HBox();
            itemLayout.setAlignment(Pos.CENTER_RIGHT); // Align contents to the right
            HBox.setHgrow(productNameLabel, Priority.ALWAYS); // Allow dynamic width for the product name

            // Add flexible space to push amountLabel to the right
            Region itemSpacer = new Region();
            HBox.setHgrow(itemSpacer, Priority.ALWAYS);

            // Add elements to item layout
            itemLayout.getChildren().addAll(productNameLabel, itemSpacer, productAmountLabel);

            printLayout.getChildren().add(itemLayout);
        }

        // Dynamic Separator
        Region separator4 = new Region();
        separator4.setMinHeight(1);
        separator4.setStyle("-fx-background-color: black;");
        printLayout.getChildren().add(separator4);

        // Financial Summary
        HBox totalLayout = new HBox();
        totalLayout.setAlignment(Pos.CENTER_RIGHT);
        Label totalLabel = new Label(String.format("Total: Rp %.2f", order.getTotal()));
        totalLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold;");
        totalLayout.getChildren().add(totalLabel);
        printLayout.getChildren().add(totalLayout);

        HBox amountLayout = new HBox();
        amountLayout.setAlignment(Pos.CENTER_RIGHT);
        Label amountPaidLabel = new Label(String.format("Amount Paid: Rp %.2f", cashGiven));
        amountPaidLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold;");
        amountLayout.getChildren().add(amountPaidLabel);
        printLayout.getChildren().add(amountLayout);

        HBox changeLayout = new HBox();
        changeLayout.setAlignment(Pos.CENTER_RIGHT);
        Label changeLabel = new Label(String.format("Change: Rp %.2f", change));
        changeLabel.setStyle("-fx-font-family: 'Helvetica';");
        changeLayout.getChildren().add(changeLabel);
        printLayout.getChildren().add(changeLayout);

        // Dynamic Separator
        Region separator5 = new Region();
        separator5.setMinHeight(1);
        separator5.setStyle("-fx-background-color: black;");
        printLayout.getChildren().add(separator5);

        // Dynamic Separator
        Region separator6 = new Region();
        separator6.setMinHeight(1);
        separator6.setStyle("-fx-background-color: black;");
        printLayout.getChildren().add(separator6);

        // Date and Timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String dateTime = java.time.LocalDateTime.now().format(formatter);

        HBox dateLayout = new HBox();
        dateLayout.setAlignment(Pos.CENTER_LEFT);
        Label dateTimeLabel = new Label("Date: " + dateTime);
        dateTimeLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 12px;");
        dateLayout.getChildren().add(dateTimeLabel);
        printLayout.getChildren().add(dateLayout);

        // Extra separator line after the date
        printLayout.getChildren().add(new Label("")); // Optional empty line

        // Thank you message (centered)
        Label thankYouLabel = new Label("Thank you for your purchase!");
        thankYouLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 12px;");
        printLayout.getChildren().add(thankYouLabel);

        // Instagram and Phone information (left-aligned with spaces)
        Label instagramLabel = new Label("Instagram: @instagram");
        instagramLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 12px;");

        Label phoneLabel = new Label("Phone: 012345");
        phoneLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 12px;");

        // Add labels to print layout
        printLayout.getChildren().addAll(instagramLabel, phoneLabel);

        // Extra line after the thank you message
        printLayout.getChildren().add(new Label("")); // Optional empty line

        return printLayout;
    }
    */

    /*
    private void showError(String message) {
        receiptLabel.setText(message);
    }
    */
}
