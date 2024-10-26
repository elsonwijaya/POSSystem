package javafx.utils;

import javafx.model.Receipt;
import javafx.model.Product;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Destination;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;

public class ThermalPrinter implements Printable {
    private static final int PRINTER_WIDTH_MM = 80;  // Standard thermal paper width
    private static final int CHAR_WIDTH = 32;        // Characters per line
    private static final float POINTS_PER_MM = 72f / 25.4f;  // Convert mm to points
    private List<String> lines;
    private Receipt receipt;

    public void printReceipt(Receipt receipt, String outputPath) throws Exception {
        this.receipt = receipt;
        this.lines = new ArrayList<>();

        // Generate content
        generateReceiptContent();

        // Setup printer job
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pageFormat = job.defaultPage();
        Paper paper = new Paper();

        // Set paper size (80mm width, height based on content)
        double width = PRINTER_WIDTH_MM * POINTS_PER_MM;
        double height = (lines.size() + 10) * 12;  // Approximate height based on line count
        paper.setSize(width, height);
        paper.setImageableArea(0, 0, width, height);

        pageFormat.setPaper(paper);
        pageFormat.setOrientation(PageFormat.PORTRAIT);

        job.setPrintable(this, pageFormat);

        // Set up PDF output
        HashPrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(new Destination(new File(outputPath).toURI()));

        // Generate PDF
        job.print(attributes);
    }

    private void generateReceiptContent() {
        // Header
        lines.add(centerText(receipt.getBusinessName()));
        lines.add(centerText(receipt.getSlogan()));
        lines.add("");

        // Invoice title
        lines.add(centerText("INVOICE"));
        lines.add("----------------------------------------");

        // Items
        for (Map.Entry<Product, Integer> entry : receipt.getOrder().getItems().entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            double totalPrice = product.getPrice() * quantity;

            lines.add(product.getName());
            lines.add(rightAlign(String.format("%dx%.2f = Rp%.2f",
                    quantity, product.getPrice(), totalPrice)));
        }

        lines.add("----------------------------------------");

        // Totals
        lines.add(rightAlign(String.format("Total: Rp%.2f", receipt.getOrder().getTotal())));
        lines.add(rightAlign(String.format("Cash: Rp%.2f", receipt.getCashGiven())));
        lines.add(rightAlign(String.format("Change: Rp%.2f", receipt.getChange())));
        lines.add("");

        // Footer
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        lines.add("Date: " + receipt.getDateTime().format(formatter));
        lines.add("");
        lines.add(centerText("Thank you for your purchase!"));
        lines.add(centerText("Instagram: " + receipt.getInstagram()));
        lines.add(centerText("Phone: " + receipt.getPhone()));
    }

    private String centerText(String text) {
        if (text.length() >= CHAR_WIDTH) return text;
        int spaces = (CHAR_WIDTH - text.length()) / 2;
        return " ".repeat(spaces) + text;
    }

    private String rightAlign(String text) {
        if (text.length() >= CHAR_WIDTH) return text;
        int spaces = CHAR_WIDTH - text.length();
        return " ".repeat(spaces) + text;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 10);
        g2d.setFont(font);

        FontRenderContext frc = g2d.getFontRenderContext();
        LineMetrics lm = font.getLineMetrics("", frc);
        float lineHeight = lm.getHeight();

        float y = lineHeight;
        for (String line : lines) {
            g2d.drawString(line, 0, y);
            y += lineHeight;
        }

        return PAGE_EXISTS;
    }
}