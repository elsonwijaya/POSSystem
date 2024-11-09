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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        lines.add(separator);
        lines.add(centerText("INVOICE"));
        lines.add(separator);

        // Column Headers
        lines.add(String.format("%-22s%10s", "Description", "Amount"));
        lines.add(separator);

        // Items - Description and Amount in columns
        for (Map.Entry<Product, Integer> entry : receipt.getOrder().getItems().entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            long totalPrice = (long)(product.getPrice() * quantity);

            // Product description
            lines.add(String.format("%s - %s",
                    product.getType().getDisplayName(),
                    product.getVariant()));

            // Price calculation right-aligned
            String priceFormat = String.format("Rp %,d", totalPrice);
            lines.add(rightAlign(priceFormat));
        }

        lines.add(separator);

        // Financial details - right aligned
        lines.add(rightAlign(String.format("Total: Rp %,d", (long)receipt.getOrder().getTotal())));

        // Use E-payment or Cash based on payment method
        if (receipt.isEPayment()) {
            lines.add(rightAlign(String.format("E-payment: Rp %,d", (long)receipt.getCashGiven())));
        } else {
            lines.add(rightAlign(String.format("Cash: Rp %,d", (long)receipt.getCashGiven())));
        }

        lines.add(rightAlign(String.format("Change: Rp %,d", (long)receipt.getChange())));
        lines.add(separator);
        lines.add(separator);

        // Date - left aligned
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        lines.add("Date: " + receipt.getDateTime().format(formatter));
        lines.add("");

        // Footer - centered
        lines.add(centerText("Thank you for your purchase!"));
        lines.add(centerText("Instagram: " + receipt.getInstagram()));
        lines.add("");
        lines.add(centerText("best served cold"));
        lines.add(centerText("please kept refrigerated"));
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

    // PDF Printing
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) return NO_SUCH_PAGE;

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 8);
        g2d.setFont(font);

        float y = g2d.getFontMetrics().getHeight();

        // Print each line exactly as formatted in generateReceiptContent
        for (String line : lines) {
            g2d.drawString(line, 0, y);  // All lines start from left margin
            y += g2d.getFontMetrics().getHeight();
        }

        return PAGE_EXISTS;
    }

    // Thermal Printing
    public void printToThermalPrinter() throws Exception {
        try {
            PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
            if (printService == null) {
                throw new Exception("No printer found");
            }

            PrinterOutputStream printerOS = new PrinterOutputStream(printService);
            EscPos escpos = new EscPos(printerOS);

            // Set styles
            Style centerStyle = new Style()
                    .setJustification(Style.Justification.Center);
            Style leftStyle = new Style()
                    .setJustification(Style.Justification.Left_Default);

            // Header
            escpos.write(centerStyle, receipt.getBusinessName()).feed(1)
                    .write(centerStyle, receipt.getSlogan()).feed(1)
                    .write(centerStyle, "-".repeat(32)).feed(1)
                    .write(centerStyle, "INVOICE").feed(1)
                    .write(centerStyle, "-".repeat(32)).feed(1)
                    .write(leftStyle, "Description Amount").feed(1)
                    .write(centerStyle, "-".repeat(32)).feed(1);

            // Items
            for (Map.Entry<Product, Integer> entry : receipt.getOrder().getItems().entrySet()) {
                Product product = entry.getKey();
                int quantity = entry.getValue();
                long totalPrice = (long)(product.getPrice() * quantity);

                // Product name
                escpos.write(leftStyle, String.format("%s - %s",
                        product.getType().getDisplayName(),
                        product.getVariant())).feed(1);

                // Price
                escpos.write(leftStyle, String.format("Rp %,d", totalPrice)).feed(1);
            }

            // Separator before totals
            escpos.write(centerStyle, "-".repeat(32)).feed(1);

            // Totals
            escpos.write(leftStyle, String.format("Total: Rp %,d", (long)receipt.getOrder().getTotal())).feed(1);

            // Use E-payment or Cash based on payment method
            if (receipt.isEPayment()) {
                escpos.write(leftStyle, String.format("E-payment: Rp %,d", (long)receipt.getCashGiven())).feed(1);
            } else {
                escpos.write(leftStyle, String.format("Cash: Rp %,d", (long)receipt.getCashGiven())).feed(1);
            }

            escpos.write(leftStyle, String.format("Change: Rp %,d", (long)receipt.getChange())).feed(1);

            // Double separator after totals
            escpos.write(centerStyle, "-".repeat(32)).feed(1)
                    .write(centerStyle, "-".repeat(32)).feed(1);

            // Date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            escpos.write(leftStyle, "Date: " + receipt.getDateTime().format(formatter)).feed(2);

            // Footer
            escpos.write(centerStyle, "Thank you for your purchase!").feed(1)
                    .write(centerStyle, "Instagram: " + receipt.getInstagram()).feed(2)
                    .write(centerStyle, "best served cold").feed(1)
                    .write(centerStyle, "please kept refrigerated").feed(3)
                    .cut(EscPos.CutMode.FULL);

            escpos.close();
            printerOS.close();

        } catch (Exception e) {
            throw new Exception("Failed to print to thermal printer: " + e.getMessage());
        }
    }

    // PDF Generation
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