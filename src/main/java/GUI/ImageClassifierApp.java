package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ImageClassifierApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/GUI/ImageClassifier.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 900, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Multi Image Classification App");
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
