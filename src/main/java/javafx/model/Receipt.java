package javafx.model;

import java.time.LocalDateTime;
import java.util.Map;

public class Receipt {
    private Order order;
    private LocalDateTime dateTime;
    private double cashGiven;
    private double change;
    private static final String BUSINESS_NAME = "businessname";
    private static final String SLOGAN = "slogan";
    private static final String INSTAGRAM = "@instagram";
    private static final String PHONE = "012345";

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