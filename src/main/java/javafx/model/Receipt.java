package javafx.model;

import java.time.LocalDateTime;
import java.util.Map;

public class Receipt {
    private Order order;
    private LocalDateTime dateTime;
    private double cashGiven;
    private double change;
    private boolean isEPayment;  // New field
    private static final String BUSINESS_NAME = "secondcourse.";
    private static final String SLOGAN = "'CAUSE FIRST IS NEVER ENOUGH'";
    private static final String INSTAGRAM = "@secondcourse.id";
    private static final String PHONE = "0123456789";

    public Receipt(Order order, double cashGiven, double change, boolean isEPayment) {  // Updated constructor
        this.order = order;
        this.dateTime = LocalDateTime.now();
        this.cashGiven = cashGiven;
        this.change = change;
        this.isEPayment = isEPayment;
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

    public boolean isEPayment() {  // New getter
        return isEPayment;
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