package model;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import org.imgscalr.Scalr;

public class ImagePreprocessor {
    public static float[][][][] preprocessImage(String imagePath) throws IOException {

        System.out.println("Processing image: " + imagePath);

        File imageFile = new File(imagePath);


        if (!imageFile.exists() || !imageFile.canRead()) {
            throw new IOException("Cannot read image file: " + imagePath);
        }

        BufferedImage img = ImageIO.read(imageFile);


        if (img == null) {
            throw new IOException("Failed to load image file.");
        }


        System.out.println("Original image size: " + img.getWidth() + "x" + img.getHeight());


        BufferedImage resizedImg = Scalr.resize(img, Scalr.Method.ULTRA_QUALITY, 224, 224, Scalr.OP_ANTIALIAS);


        if (resizedImg.getWidth() < 224 || resizedImg.getHeight() < 224) {
            BufferedImage paddedImage = new BufferedImage(224, 224, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = paddedImage.createGraphics();


            int xPad = (224 - resizedImg.getWidth()) / 2;
            int yPad = (224 - resizedImg.getHeight()) / 2;


            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 224, 224);


            g.drawImage(resizedImg, xPad, yPad, null);
            g.dispose();

            resizedImg = paddedImage;
        }


        System.out.println("Resized (or padded) image size: " + resizedImg.getWidth() + "x" + resizedImg.getHeight());

        float[][][][] inputArray = new float[1][224][224][3]; // Batch size of 1
        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 224; x++) {
                int rgb = resizedImg.getRGB(x, y);
                inputArray[0][y][x][0] = ((rgb >> 16) & 0xFF) / 255.0f; // Red
                inputArray[0][y][x][1] = ((rgb >> 8) & 0xFF) / 255.0f;  // Green
                inputArray[0][y][x][2] = (rgb & 0xFF) / 255.0f;          // Blue
            }
        }
        return inputArray;
    }
}
