package com.compressionWithBytes;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestLZSS {
    public static final String CSV_FILEPATH = "coefficient_addiction.csv";

    public static void test(String inputFilePath, String outputFilePath, int bytes){
        String decodedFilePath = inputFilePath;
        int dotIndex = inputFilePath.indexOf(".");
        if (dotIndex > 0){
            outputFilePath = inputFilePath.substring(0, dotIndex) + "_"  + bytes + "ENCODED" + inputFilePath.substring(dotIndex);
        }
        else {
            outputFilePath = inputFilePath + "_" + bytes + "_ENCODED";
        }
        try(InputStream inputStream = new FileInputStream(inputFilePath); OutputStream outputStream = new FileOutputStream(outputFilePath)){
            LZSSBytes.WINDOW_SIZE = bytes;
            LZSSBytes.encode(inputStream, outputStream);;
        }
        catch (IOException e){
            e.printStackTrace();
        }
        CalculateCoefficient(inputFilePath, outputFilePath, bytes);
    }

    public static void CalculateCoefficient(String inputFilePath, String outputFilePath, int bytes) {
        try {
            long originalSize = Files.size(Paths.get(inputFilePath));
            long compressedSize = Files.size(Paths.get(outputFilePath));

            double compressionRatio = (double) originalSize / compressedSize;
            double compressionPercentage = (1 - (double) compressedSize / originalSize) * 100;

            String csvLine = bytes + "," + compressionRatio + "," + compressionPercentage + "\n";
            
            boolean fileExists = new File(CSV_FILEPATH).exists();
            try (FileWriter writer = new FileWriter(CSV_FILEPATH, true)) {
                if (!fileExists) {
                    writer.write("Buffer Size,Compression Ratio,Compression Percentage\n");
                }
                writer.write(csvLine);
            }

            System.out.printf("Buffer: %d | Коэффициент сжатия: %.2f | Процент сжатия: %.2f%%%n",
                    bytes, compressionRatio, compressionPercentage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        String inputFilePath = ("dataset/enwik7");
        String outputFilePath = ("");

        int bytes = 256;

        while (bytes < 40000) {
            test(inputFilePath, outputFilePath, bytes);
            bytes *= 2;
        }
    }
}
