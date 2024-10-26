package javafx.utils;

import javafx.MainApp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneUtil {

    // Constants for window dimensions (optional)
    public static final int DEFAULT_WINDOW_WIDTH = 800;
    public static final int DEFAULT_WINDOW_HEIGHT = 600;
    // Utility method to change the scene
    public static void switchScene(String fxmlFileName) {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(SceneUtil.class.getResource("/javafx/" + fxmlFileName));
            Parent root = loader.load();

            // Get the primary stage (assuming you use MainApp to get the stage)
            Stage stage = MainApp.getPrimaryStage();

            // Create a new scene with default dimensions (can be customized)
            Scene scene = new Scene(root, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);

            // Set the scene and show it
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();  // Handle the exception or log it
        }
    }
}
