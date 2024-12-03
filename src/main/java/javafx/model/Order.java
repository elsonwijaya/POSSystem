package javafx.model;

import java.util.HashMap;
import java.util.Map;

public class Order {
    private Map<Product, Integer> items;
    private static final double DISCOUNT_THRESHOLD_1 = 300000.0;
    private static final double DISCOUNT_THRESHOLD_2 = 500000.0;
    private static final double DISCOUNT_RATE_1 = 0.05; // 5%
    private static final double DISCOUNT_RATE_2 = 0.10; // 10%

    public Order() {
        this.items = new HashMap<>();
    }

    public void addItem(Product product, int quantity) {
        items.put(product, items.getOrDefault(product, 0) + quantity);
    }

    public Map<Product, Integer> getItems() {
        return items;
    }

    public double getSubtotal() {
        double subtotal = 0;
        for (Map.Entry<Product, Integer> entry : items.entrySet()) {
            subtotal += entry.getKey().getPrice() * entry.getValue();
        }
        return subtotal;
    }

    public double getDiscount() {
        double subtotal = getSubtotal();
        if (subtotal >= DISCOUNT_THRESHOLD_2) {
            return subtotal * DISCOUNT_RATE_2;
        } else if (subtotal >= DISCOUNT_THRESHOLD_1) {
            return subtotal * DISCOUNT_RATE_1;
        }
        return 0;
    }

    public double getTotal() {
        return getSubtotal() - getDiscount();
    }

    public String getDiscountDescription() {
        double subtotal = getSubtotal();
        if (subtotal >= DISCOUNT_THRESHOLD_2) {
            return "10% Discount Applied";
        } else if (subtotal >= DISCOUNT_THRESHOLD_1) {
            return "5% Discount Applied";
        }
        return "";
    }
}