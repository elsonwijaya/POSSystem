package database;

import javafx.model.Product;
import javafx.model.Order;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Handles all database operations and initialisation
public class Database {
    private static Connection connection;
    private static String databaseUrl;

    private Database() {}

    public static void initialize() throws SQLException {
        try {
            setupDatabasePath();
            connection = getConnection();
            setup();
        } catch (Exception e) {
            System.err.println("Critical error during database initialization:");
            e.printStackTrace();
            throw new SQLException("Failed to initialize database: " + e.getMessage(), e);
        }
    }

    private static void setupDatabasePath() throws IOException, SQLException {
        try {
            // Define AppData path
            String appDataPath = System.getenv("APPDATA") + File.separator + "POSSystem";
            File appDataDir = new File(appDataPath);
            System.out.println("AppData directory exists: " + appDataDir.exists());
            System.out.println("AppData directory is writable: " + appDataDir.canWrite());
            Path dbDirectory = Paths.get(appDataPath);
            Path dbPath = dbDirectory.resolve("app_database.db");

            System.out.println("Setting up database...");
            System.out.println("AppData path: " + appDataPath);
            System.out.println("Database path: " + dbPath);
            System.out.println("Full database path: " + dbPath.toAbsolutePath().toString());

            // Create directory if it doesn't exist
            if (!Files.exists(dbDirectory)) {
                System.out.println("Creating database directory...");
                Files.createDirectories(dbDirectory);
            }

            // Check if database exists in AppData
            if (!Files.exists(dbPath)) {
                System.out.println("Database not found in AppData, copying from resources...");

                // Copy from resources if it doesn't exist
                try (InputStream is = Database.class.getResourceAsStream("/db/app_database.db")) {
                    if (is != null) {
                        Files.copy(is, dbPath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Database copied successfully to: " + dbPath);
                    } else {
                        System.out.println("No template database found in resources, creating new one...");
                        // Create an empty database file
                        Files.createFile(dbPath);
                        // Set the database URL and initialize it
                        databaseUrl = "jdbc:sqlite:" + dbPath.toString();
                        Connection conn = getConnection();
                        setup();  // This will create all necessary tables
                        System.out.println("Created and initialized new database file at: " + dbPath);
                        return; // Exit after creating new database
                    }
                }
            }

            // Set the database URL to the AppData location
            databaseUrl = "jdbc:sqlite:" + dbPath.toString();
            System.out.println("Database URL set to: " + databaseUrl);

        } catch (Exception e) {
            System.err.println("Error setting up database path: " + e.getMessage());
            e.printStackTrace();
            throw e; // Rethrow to handle in initialize()
        }
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(databaseUrl);
        }
        return connection;
    }

    public static void setup() throws SQLException {
        // Step 1: Establish a connection
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            // Step 2: Create the products table if it doesn't exist
            String createProductsTableSQL = """
        CREATE TABLE IF NOT EXISTS products (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT UNIQUE NOT NULL,
            price REAL NOT NULL
        );
        """;

            // Step 3: Create the orders table if it doesn't exist
            String createOrdersTableSQL = """
        CREATE TABLE IF NOT EXISTS orders (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            total REAL NOT NULL,
            order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        """;

            // Step 4: Create the order_items table (for each product sold in an order) if it doesn't exist
            String createOrderItemsTableSQL = """
        CREATE TABLE IF NOT EXISTS order_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            order_id INTEGER NOT NULL,  -- The order this item belongs to
            product_id INTEGER NOT NULL,  -- The product being sold
            quantity INTEGER NOT NULL,  -- Number of units of the product sold
            price_per_unit REAL NOT NULL,  -- Price per unit at the time of sale
            FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
            FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
        );
        """;


            // Step 4: Execute the SQL statements to create the tables
            statement.execute(createProductsTableSQL);
            statement.execute(createOrdersTableSQL);
            statement.execute(createOrderItemsTableSQL);

            System.out.println("Database setup complete. Tables created if they didn't exist.");

        } catch (SQLException e) {
            throw new SQLException("Error during database setup: " + e.getMessage(), e);
        }
    }

    public static void addProduct(Product product) throws SQLException {
        String sql = "INSERT INTO products (name, price) VALUES (?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, product.getName());
            pstmt.setDouble(2, product.getPrice());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException("Error adding product: " + e.getMessage(), e);
        }
    }

    public static boolean removeProduct(Product product) throws SQLException {
        String sql = "DELETE FROM products WHERE name = ? AND price = ?"; // Using name and price to identify the product

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getName());
            pstmt.setDouble(2, product.getPrice()); // Ensure `price` uniquely identifies products if names may overlap
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;  // Returns true if the product was removed successfully
        } catch (SQLException e) {
            System.err.println("Error removing product: " + e.getMessage());
            throw new SQLException("Failed to remove product: " + e.getMessage(), e);
        }
    }

    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT name, price FROM products";

        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                products.add(new Product(name, price));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching products: " + e.getMessage());
        }
        return products;
    }

    public static long saveOrder(Order order) throws SQLException {
        String sql = "INSERT INTO orders (total) VALUES (?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setDouble(1, order.getTotal());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating order failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long orderId = generatedKeys.getLong(1);
                    // Save order items
                    saveOrderItems(orderId, order.getItems());
                    return orderId;
                } else {
                    throw new SQLException("Creating order failed, no ID obtained.");
                }
            }
        }
    }

    private static void saveOrderItems(long orderId, Map<Product, Integer> items) throws SQLException {
        String sql = """
        INSERT INTO order_items (order_id, product_id, quantity, price_per_unit)
        SELECT ?, id, ?, price
        FROM products
        WHERE name = ?
    """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Map.Entry<Product, Integer> entry : items.entrySet()) {
                Product product = entry.getKey();
                int quantity = entry.getValue();

                pstmt.setLong(1, orderId);
                pstmt.setInt(2, quantity);
                pstmt.setString(3, product.getName());

                pstmt.executeUpdate();
            }
        }
    }

    // Method to retrieve order history
    public static List<Map<String, Object>> getOrderHistory() throws SQLException {
        List<Map<String, Object>> orderHistory = new ArrayList<>();

        String sql = """
        SELECT o.id, o.total, o.order_date,
               oi.quantity, oi.price_per_unit,
               p.name as product_name
        FROM orders o
        JOIN order_items oi ON o.id = oi.order_id
        JOIN products p ON oi.product_id = p.id
        ORDER BY o.order_date DESC
    """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            long currentOrderId = -1;
            Map<String, Object> currentOrder = null;
            List<Map<String, Object>> currentItems = null;

            while (rs.next()) {
                long orderId = rs.getLong("id");

                if (orderId != currentOrderId) {
                    // Create new order entry
                    currentOrder = new HashMap<>();
                    currentItems = new ArrayList<>();
                    currentOrder.put("orderId", orderId);
                    currentOrder.put("total", rs.getDouble("total"));
                    currentOrder.put("date", rs.getTimestamp("order_date"));
                    currentOrder.put("items", currentItems);
                    orderHistory.add(currentOrder);
                    currentOrderId = orderId;
                }

                // Add item to current order
                Map<String, Object> item = new HashMap<>();
                item.put("productName", rs.getString("product_name"));
                item.put("quantity", rs.getInt("quantity"));
                item.put("pricePerUnit", rs.getDouble("price_per_unit"));
                currentItems.add(item);
            }
        }

        return orderHistory;
    }





    public static String generateReceiptFileName(String type) {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = now.format(DateTimeFormatter.ofPattern("HHmm"));

        String baseFileName = String.format("%s_%s_%s", type, date, time);
        String fileName = baseFileName + ".pdf";
        int counter = 1;

        // Check if file exists and append counter if it does
        File file = new File(getAppDirectory() + File.separator + fileName);
        while (file.exists()) {
            fileName = baseFileName + "_" + counter + ".pdf";
            file = new File(getAppDirectory() + File.separator + fileName);
            counter++;
        }

        return fileName;
    }

    public static String getAppDirectory() {
        String appPath;

        // Check if we're in development environment
        boolean isDevelopment = System.getProperty("java.class.path").contains("gradle");

        if (isDevelopment) {
            // Development environment - use build directory
            appPath = System.getProperty("user.dir") + File.separator + "build";
            System.out.println("Running in development mode");
        } else {
            // Production environment - use installation directory
            appPath = System.getProperty("user.dir");
            System.out.println("Running in production mode");
        }

        // Create Receipt History folder
        File receiptDir = new File(appPath, "Receipt History");
        if (!receiptDir.exists()) {
            receiptDir.mkdirs();
        }

        System.out.println("Receipt directory: " + receiptDir.getAbsolutePath());  // For debugging
        return receiptDir.getAbsolutePath();
    }
}
