package javafx;

import database.Database;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static javafx.utils.SceneUtil.DEFAULT_WINDOW_HEIGHT;
import static javafx.utils.SceneUtil.DEFAULT_WINDOW_WIDTH;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        try {
            logSystemInfo();

            // Initialize database with error handling
            try {
                System.out.println("Initializing database...");
                Database.initialize();
                System.out.println("Database initialization successful");
            } catch (Exception e) {
                System.err.println("Database initialization failed: " + e.getMessage());
                e.printStackTrace();
                showError("Database Error", "Failed to initialize database: " + e.getMessage());
                return;
            }

            // Store the primary stage reference
            primaryStage = stage;

            // Load the home page scene with error handling
            Parent root;
            try {
                System.out.println("Loading FXML...");
                String fxmlPath = "/javafx/HomePage.fxml";
                var resource = getClass().getResource(fxmlPath);
                if (resource == null) {
                    throw new IOException("Cannot find resource: " + fxmlPath);
                }
                root = FXMLLoader.load(resource);
                System.out.println("FXML loaded successfully");
            } catch (Exception e) {
                System.err.println("FXML loading failed: " + e.getMessage());
                e.printStackTrace();
                showError("Loading Error", "Failed to load application interface: " + e.getMessage());
                return;
            }

            // Load CSS with error handling
            try {
                System.out.println("Loading CSS...");
                String cssPath = "/css/HomePage.css";
                var cssResource = getClass().getResource(cssPath);
                if (cssResource == null) {
                    throw new IOException("Cannot find resource: " + cssPath);
                }
                Scene scene = new Scene(root, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
                scene.getStylesheets().add(cssResource.toExternalForm());
                System.out.println("CSS loaded successfully");

                primaryStage.setTitle("POS");
                primaryStage.setScene(scene);
                primaryStage.show();
            } catch (Exception e) {
                System.err.println("CSS loading failed: " + e.getMessage());
                e.printStackTrace();
                showError("Style Error", "Failed to load application styles: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Application start failed: " + e.getMessage());
            e.printStackTrace();
            showError("Startup Error", "Failed to start application: " + e.getMessage());
        }
    }

    private void logSystemInfo() {
        System.out.println("Application starting...");
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("JavaFX Version: " + System.getProperty("javafx.version"));
        System.out.println("OS: " + System.getProperty("os.name"));

        // Log important paths
        String appData = System.getenv("APPDATA");
        System.out.println("AppData Path: " + appData);

        // Log class path and module path
        System.out.println("Class Path: " + System.getProperty("java.class.path"));
        System.out.println("Module Path: " + System.getProperty("jdk.module.path"));
    }

    private void showError(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
            Platform.exit();
        });
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        System.out.println("Starting application with args: " + String.join(", ", args));
        launch(args);
    }
}