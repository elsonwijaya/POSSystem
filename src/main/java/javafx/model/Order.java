package javafx.model;

import java.util.HashMap;
import java.util.Map;

public class Order {
    private Map<Product, Integer> items; // Key: Product, Value: Quantity
    private double total;

    public Order() {
        this.items = new HashMap<>();
    }

    public void addItem(Product product, int quantity) {
        items.put(product, items.getOrDefault(product, 0) + quantity); // Update quantity
        total += product.getPrice() * quantity; // Update total
    }

    public Map<Product, Integer> getItems() {
        return items;
    }

    public double getTotal() {
        double total = 0;
        for (Map.Entry<Product, Integer> entry : items.entrySet()) {
            total += entry.getKey().getPrice() * entry.getValue();
        }
        return total;
    }
}
