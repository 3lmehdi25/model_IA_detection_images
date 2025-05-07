package model;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ModelClassifier {

    private Graph graph;
    private List<String> labels;
    private static final String MODEL_PATH = "src/main/resources/model/tensorflow_inception_graph.pb";


    public ModelClassifier(String modelPath, String labelFilePath) {
        loadModel(modelPath);
        loadLabels(labelFilePath);
    }


    private void loadModel(String modelPath) {
        try {
            byte[] graphDef = Files.readAllBytes(Paths.get(modelPath));
            graph = new Graph();
            graph.importGraphDef(graphDef);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load the model: " + e.getMessage(), e);
        }
    }


    private void loadLabels(String labelFilePath) {
        try {
            labels = Files.readAllLines(Paths.get(labelFilePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load label file: " + e.getMessage(), e);
        }
    }

    public String classifyImage(float[][][][] inputData, String inputLayer, String outputLayer) {
        try (Graph graph = new Graph()) {

            byte[] graphDef = Files.readAllBytes(Paths.get(MODEL_PATH));
            graph.importGraphDef(graphDef);

            try (Session session = new Session(graph)) {

                Tensor<?> inputTensor = Tensor.create(inputData, Float.class);


                Tensor<?> result = session.runner()
                        .feed(inputLayer, inputTensor)
                        .fetch("softmax2")
                        .run().get(0);

                long[] shape = result.shape();
                System.out.println("Output tensor shape: " + Arrays.toString(shape));


                float[] probabilities = new float[(int) shape[1]];
                result.copyTo(new float[][]{probabilities});


                int bestClassIndex = getBestLabelIndex(probabilities);
                float bestProb = probabilities[bestClassIndex];


                String className = labels.get(bestClassIndex);


                return className + " " + bestProb*100 + "%";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }





    private int getBestLabelIndex(float[] probabilities) {
        int bestIndex = 0;
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > probabilities[bestIndex]) {
                bestIndex = i;
            }
        }
        return bestIndex;
    }

}
