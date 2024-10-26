package database;

import javafx.model.Product;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public static void setTestDbUrl(String testDbUrl) {
        databaseUrl = testDbUrl;
    }


}
