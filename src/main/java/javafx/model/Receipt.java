package javafx.model;

import java.time.LocalDateTime;
import java.util.Map;

public class Receipt {
    private Order order;
    private LocalDateTime dateTime;
    private double cashGiven;
    private double change;
    // Updated with actual values
    private static final String BUSINESS_NAME = "secondcourse.";    // Replace with actual business name
    private static final String SLOGAN = "'CAUSE FIRST IS NEVER ENOUGH'";        // Replace with actual slogan
    private static final String INSTAGRAM = "@secondcourse.id";            // Replace with actual Instagram
    private static final String PHONE = "0123456789";                   // Replace with actual phone number

    public Receipt(Order order, double cashGiven, double change) {
        this.order = order;
        this.dateTime = LocalDateTime.now();
        this.cashGiven = cashGiven;
        this.change = change;
    }

    public Order getOrder() {
        return order;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public double getCashGiven() {
        return cashGiven;
    }

    public double getChange() {
        return change;
    }

    public String getBusinessName() {
        return BUSINESS_NAME;
    }

    public String getSlogan() {
        return SLOGAN;
    }

    public String getInstagram() {
        return INSTAGRAM;
    }

    public String getPhone() {
        return PHONE;
    }
}