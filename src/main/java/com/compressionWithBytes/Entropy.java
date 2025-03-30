package com.compressionWithBytes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Entropy {
    public static double calculateEntropy(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        Map<Byte, Integer> frequencyMap = new HashMap<>();
        int totalBytes = 0;

        int byteRead;
        while ((byteRead = fis.read()) != -1) {
            byte b = (byte) byteRead;
            frequencyMap.put(b, frequencyMap.getOrDefault(b, 0) + 1);
            totalBytes++;
        }
        fis.close();

        double entropy = 0.0;
        for (int count : frequencyMap.values()) {
            double probability = (double) count / totalBytes;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }

        return entropy;
    }

    public static void main(String[] args) throws IOException {
        String filePath = "bw.raw";

        double entropy = calculateEntropy(filePath);
        System.out.println(entropy);
    }
}
