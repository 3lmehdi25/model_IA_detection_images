package GUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.ImagePreprocessor;
import model.ModelClassifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ImageClassifierController {

    @FXML
    private TilePane imageDisplayPane;

    @FXML
    private Button loadButton;

    @FXML
    private Button classifyButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button resetButton;

    @FXML
    private ProgressIndicator loadingIndicator;

    private final List<VBox> selectedImages = new ArrayList<>();
    private final ModelClassifier modelClassifier;

    public ImageClassifierController() {

        modelClassifier = new ModelClassifier(
                "src/main/resources/model/tensorflow_inception_graph.pb",
                "src/main/resources/model/imagenet_comp_graph_label_strings.txt"
        );
    }

    @FXML
    public void initialize() {

        loadButton.setOnAction(e -> loadImages());
        classifyButton.setOnAction(e -> initiateClassification());
        deleteButton.setOnAction(e -> deleteSelectedImages());
        resetButton.setOnAction(e -> resetClassifications());
    }

    private void loadImages() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        fileChooser.setTitle("Select Images");

        List<File> files = fileChooser.showOpenMultipleDialog(null);
        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                try {
                    Image originalImage = new Image(file.toURI().toString());
                    Image resizedImage = new Image(file.toURI().toString(), 150, 150, false, true);


                    ImageView imageView = createSelectableImageView(resizedImage);


                    Label classificationLabel = new Label("Type: Unknown");
                    classificationLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");


                    VBox vBox = new VBox(5);
                    vBox.setAlignment(Pos.CENTER);
                    vBox.getChildren().addAll(imageView, classificationLabel);


                    imageDisplayPane.getChildren().add(vBox);
                } catch (Exception e) {
                    showError("Error loading image: " + file.getName() + " - " + e.getMessage());
                }
            }
        }
    }




    private ImageView createSelectableImageView(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(100);
        imageView.setPreserveRatio(true);
        imageView.setOnMouseClicked(event -> handleImageSelection(event, imageView));
        return imageView;
    }


    private void handleImageSelection(MouseEvent event, ImageView imageView) {
        VBox imagePane = (VBox) imageView.getParent();

        if (selectedImages.contains(imagePane)) {
            selectedImages.remove(imagePane);
            imagePane.getStyleClass().remove("image-box-selected");
        } else {
            selectedImages.add(imagePane);
            if (!imagePane.getStyleClass().contains("image-box-selected")) {
                imagePane.getStyleClass().add("image-box-selected");
            }
        }
    }


    private void deleteSelectedImages() {

        imageDisplayPane.getChildren().removeAll(selectedImages);
        selectedImages.clear();
    }


    private void resetClassifications() {
        for (var node : imageDisplayPane.getChildren()) {
            if (node instanceof VBox imagePane) {
                Label classificationLabel = (Label) imagePane.getChildren().get(1);
                classificationLabel.setText("Type: Unknown");
            }
        }
        selectedImages.clear();

        imageDisplayPane.getChildren().forEach(image -> ((VBox) image).getStyleClass().remove("image-box-selected"));
    }


    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Image Load Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void classifyImageTask(String imagePath, Label classificationLabel) {
        try {

            float[][][][] inputData = preprocessImageInParallel(imagePath);
            String result = modelClassifier.classifyImage(inputData, "input", "softmax2");

            Platform.runLater(() -> classificationLabel.setText(result));
        } catch (IOException e) {
            Platform.runLater(() -> classificationLabel.setText("Error: " + e.getMessage()));
        }
    }

    private float[][][][] preprocessImageInParallel(String imagePath) throws IOException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ImagePreprocessor.preprocessImage(imagePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).join();
    }



    private void initiateClassification() {
        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(10, Runtime.getRuntime().availableProcessors()));


        loadingIndicator.setVisible(true);

        for (var node : imageDisplayPane.getChildren()) {
            if (node instanceof VBox imagePane) {
                ImageView imageView = (ImageView) imagePane.getChildren().get(0);
                Label classificationLabel = (Label) imagePane.getChildren().get(1);

                String imagePath = ((Image) imageView.getImage()).getUrl().replace("file:/", "");


                executorService.submit(() -> classifyImageTask(imagePath, classificationLabel));
            }
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            showError("Image classification interrupted.");
        } finally {
            loadingIndicator.setVisible(false);
        }
    }

}
