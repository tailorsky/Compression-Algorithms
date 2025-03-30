package com.compressionWithBytes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.*;

public class BlackAndWhite {
    public static void main(String[] args) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите изображение");
        if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            System.out.println("Файл не выбран.");
            return;
        }
        
        File inputFile = fileChooser.getSelectedFile();
        File outputFile = new File(inputFile.getParent(), "output.png");
        
        try {
            BufferedImage image = ImageIO.read(inputFile);
            BufferedImage bwImage = convertToBlackAndWhite(image);
            ImageIO.write(bwImage, "png", outputFile);
            System.out.println("Конвертация завершена. Файл сохранён: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Ошибка обработки изображения: " + e.getMessage());
        }
    }

    private static BufferedImage convertToBlackAndWhite(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage bwImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                int grayscale = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
                int threshold = 128;
                int bwColor = (grayscale < threshold) ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
                bwImage.setRGB(x, y, bwColor);
            }
        }
        
        return bwImage;
    }
}