package javafx.utils;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javafx.model.Product;
import javafx.model.Receipt;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Destination;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ThermalPrinter implements Printable {
    private static final int PRINTER_WIDTH_MM = 58;
    private static final int CHAR_WIDTH = 32;
    private static final float POINTS_PER_MM = 72f / 25.4f;
    private List<String> lines;
    private Receipt receipt;

    public ThermalPrinter(Receipt receipt) {
        this.receipt = receipt;
        this.lines = new ArrayList<>();
    }

    private void generateReceiptContent() {
        String separator = "-".repeat(32);

        // Header
        lines.add(centerText(receipt.getBusinessName()));
        lines.add(centerText(receipt.getSlogan()));
        lines.add(centerText("Instagram: " + receipt.getInstagram()));
        lines.add(separator);
        lines.add(centerText("INVOICE"));
        lines.add(separator);

        // Fixed Column Headers - using specific spacing
        lines.add(String.format("%-22s%10s", "Description", "Amount"));
        lines.add(separator);

        // Sort and add items
        List<Map.Entry<Product, Integer>> sortedItems = receipt.getOrder().getItems().entrySet()
                .stream()
                .sorted((a, b) -> {
                    String productA = a.getKey().getType().getDisplayName() + a.getKey().getVariant();
                    String productB = b.getKey().getType().getDisplayName() + b.getKey().getVariant();
                    return productA.compareTo(productB);
                })
                .collect(Collectors.toList());

        // Items
        for (Map.Entry<Product, Integer> entry : sortedItems) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            long price = (long)product.getPrice();
            long totalPrice = price * quantity;

            lines.add(String.format("%s - %s",
                    product.getType().getDisplayName(),
                    product.getVariant()));
            lines.add(rightAlign(String.format("%dx%,d = Rp %,d",
                    quantity, price, totalPrice)));
        }

        lines.add(separator);

        // Get subtotal and check for discount
        double subtotal = receipt.getOrder().getSubtotal();
        lines.add(rightAlign(String.format("Total: Rp %,d", (long)subtotal)));

        // Add discount line only if applicable
        if (subtotal >= 300000) {
            String discountText = subtotal >= 500000 ? "10%" : "5%";
            double discountAmount = subtotal >= 500000 ? subtotal * 0.1 : subtotal * 0.05;
            lines.add(rightAlign(String.format("Discount (%s): -Rp %,d", discountText, (long)discountAmount)));
        }

        // Payment and change lines
        if (receipt.isEPayment()) {
            lines.add(rightAlign(String.format("E-payment: Rp %,d", (long)receipt.getCashGiven())));
        } else {
            lines.add(rightAlign(String.format("Cash: Rp %,d", (long)receipt.getCashGiven())));
        }
        lines.add(rightAlign(String.format("Change: Rp %,d", (long)receipt.getChange())));

        lines.add(separator);
        lines.add(separator);

        // Date and footer
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        lines.add("Date: " + LocalDateTime.now().format(formatter));
        lines.add("");
        lines.add(centerText("Thank you for your purchase!"));
        lines.add("");
        lines.add(centerText("Best served cold"));
        lines.add(centerText("Please kept refrigerated"));
    }

    public void printToThermalPrinter() throws Exception {
        try {
            PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
            if (printService == null) throw new Exception("No printer found");

            PrinterOutputStream printerOS = new PrinterOutputStream(printService);
            EscPos escpos = new EscPos(printerOS);

            Style centerStyle = new Style().setJustification(Style.Justification.Center);
            Style rightStyle = new Style().setJustification(Style.Justification.Right);
            Style leftStyle = new Style().setJustification(Style.Justification.Left_Default);
            Style titleStyle = new Style().setJustification(Style.Justification.Center).setFontSize(Style.FontSize._2, Style.FontSize._2);
            Style extraSmallStyle = new Style().setJustification(Style.Justification.Left_Default).setFontSize(Style.FontSize._1, Style.FontSize._1).setFontName(Style.FontName.Font_B);

            // Header
            escpos.writeLF(titleStyle, receipt.getBusinessName())
                    .writeLF(extraSmallStyle, receipt.getSlogan())
                    .writeLF(extraSmallStyle, "Instagram: " + receipt.getInstagram())
                    .writeLF(centerStyle, "-".repeat(32))
                    .writeLF(centerStyle, "INVOICE")
                    .writeLF(centerStyle, "-".repeat(32));

            // Description and Amount headers
            escpos.writeLF(extraSmallStyle, String.format("%-22s%10s", "Description", "Amount"))
                    .writeLF(centerStyle, "-".repeat(32));

            // Items
            List<Map.Entry<Product, Integer>> sortedItems = receipt.getOrder().getItems().entrySet()
                    .stream()
                    .sorted((a, b) -> {
                        String productA = a.getKey().getType().getDisplayName() + a.getKey().getVariant();
                        String productB = b.getKey().getType().getDisplayName() + b.getKey().getVariant();
                        return productA.compareTo(productB);
                    })
                    .collect(Collectors.toList());

            for (int i = 0; i < sortedItems.size(); i++) {
                Map.Entry<Product, Integer> entry = sortedItems.get(i);
                Product product = entry.getKey();
                int quantity = entry.getValue();
                long price = (long)product.getPrice();
                long totalPrice = price * quantity;

                escpos.writeLF(extraSmallStyle, String.format("%s - %s",
                        product.getType().getDisplayName(),
                        product.getVariant()));
                escpos.writeLF(extraSmallStyle, String.format("%dx%,d = Rp %,d",
                        quantity, price, totalPrice));

                // Add new line
                if (i < sortedItems.size() - 1) {
                    escpos.feed(1);
                }
            }

            escpos.writeLF(centerStyle, "-".repeat(32));

            // Updated totals section with conditional discount
            double subtotal = receipt.getOrder().getSubtotal();
            escpos.writeLF(rightStyle, String.format("Total: Rp %,d", (long)subtotal));

            // Add discount line only if applicable
            if (subtotal >= 300000) {
                String discountText = subtotal >= 500000 ? "10%" : "5%";
                double discountAmount = subtotal >= 500000 ? subtotal * 0.1 : subtotal * 0.05;
                escpos.writeLF(rightStyle, String.format("Discount (%s): -Rp %,d", discountText, (long)discountAmount));
            }

            // Payment and change
            escpos.writeLF(rightStyle, String.format("%s: Rp %,d",
                    receipt.isEPayment() ? "E-payment" : "Cash",
                    (long)receipt.getCashGiven()));
            escpos.writeLF(rightStyle, String.format("Change: Rp %,d", (long)receipt.getChange()));

            // Footer
            escpos.writeLF(centerStyle, "-".repeat(32))
                    .writeLF(centerStyle, "-".repeat(32));

            // Date
            escpos.writeLF(extraSmallStyle, "Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .feed(1);

            // Footer
            escpos.writeLF(centerStyle, "Thank you for your purchase!")
                    .feed(1)
                    .writeLF(centerStyle, "Best served cold")
                    .writeLF(centerStyle, "Please keep refrigerated")
                    .feed(3)
                    .cut(EscPos.CutMode.FULL);

            escpos.close();
            printerOS.close();

        } catch (Exception e) {
            throw new Exception("Failed to print to thermal printer: " + e.getMessage());
        }
    }

    private String centerText(String text) {
        if (text.length() >= CHAR_WIDTH) return text;
        int spaces = (CHAR_WIDTH - text.length()) / 2;
        return " ".repeat(spaces) + text;
    }

    private String rightAlign(String text) {
        if (text.length() >= CHAR_WIDTH) return text;
        return " ".repeat(CHAR_WIDTH - text.length()) + text;
    }

    // PDF Printing and Generation methods remain unchanged...
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) return NO_SUCH_PAGE;

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 8);
        g2d.setFont(font);

        float y = g2d.getFontMetrics().getHeight();

        for (String line : lines) {
            g2d.drawString(line, 0, y);
            y += g2d.getFontMetrics().getHeight();
        }

        return PAGE_EXISTS;
    }

    public void printReceipt(Receipt receipt, String outputPath) throws Exception {
        generateReceiptContent();

        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pageFormat = job.defaultPage();
        Paper paper = new Paper();

        double width = PRINTER_WIDTH_MM * POINTS_PER_MM;
        double height = (lines.size() + 5) * 12;
        paper.setSize(width, height);
        paper.setImageableArea(0, 0, width, height);

        pageFormat.setPaper(paper);
        pageFormat.setOrientation(PageFormat.PORTRAIT);
        job.setPrintable(this, pageFormat);

        HashPrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(new Destination(new File(outputPath).toURI()));

        job.print(attributes);
    }
}