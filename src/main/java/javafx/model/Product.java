package javafx.model;

public class Product {
    private String variant;
    private double price;
    private ProductType type;

    public Product(ProductType type, String variant, double price) {
        this.type = type;
        this.variant = variant;
        this.price = price;
    }

    public String getVariant() {
        return variant;
    }

    public double getPrice() {
        return price;
    }

    public ProductType getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (Rp %,.0f)", type.getDisplayName(), variant, price);
    }
}