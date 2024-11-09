package javafx.model;

public enum ProductType {
    CUBE_CAKE("Cube Cake"),
    TART("Tart"),
    PUDDING("Pudding");

    private final String displayName;

    ProductType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
