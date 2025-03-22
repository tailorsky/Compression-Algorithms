package com.compressionWithBytes;

import java.io.*;
import java.util.Locale;

public class EntropyTest {
    private static final int BUFFER_SIZE = 4096;
    private static final String CSV_FILE = "entropy_results.csv";

    public static double calculateEntropy(File file) throws IOException {
        int[] freq = new int[256];
        int totalBytes = 0;

        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    freq[buffer[i] & 0xFF]++;
                }
                totalBytes += bytesRead;
            }
        }

        if (totalBytes == 0) return 0.0;

        double entropy = 0.0;
        for (int count : freq) {
            if (count > 0) {
                double probability = (double) count / totalBytes;
                entropy += probability * (Math.log(probability) / Math.log(2));
            }
        }
        return -entropy;
    }

    public static void saveToCSV(int bytes, double entropy, boolean firstRun) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_FILE, !firstRun))) {
            if (firstRun) {
                writer.write("Bytes,Entropy\n"); // Заголовок
            }
            writer.write(String.format(Locale.US, "%d,%.4f\n", bytes, entropy)); // Запись с точкой
        } catch (IOException e) {
            System.err.println("Ошибка записи в CSV-файл.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int bytes = 1024;
        boolean firstRun = true;

        for (int i = 0; i < 5; i++) {
            File testFile = new File("enwik7_" + bytes + ".bwt.mtf");
            if (!testFile.exists()) {
                System.out.println("Файл " + testFile.getName() + " не найден.");
                bytes *= 2;
                continue;
            }

            try {
                double entropy = calculateEntropy(testFile);
                System.out.printf("Файл: %s | Размер: %d байт | Энтропия: %.4f бит/байт%n",
                        testFile.getName(), testFile.length(), entropy);

                saveToCSV(bytes, entropy, firstRun);
                firstRun = false; // После первой итерации в CSV уже не пишем заголовок
            } catch (IOException e) {
                System.err.println("Ошибка при обработке файла " + testFile.getName());
                e.printStackTrace();
            }

            bytes *= 2;
        }

        System.out.println("Результаты записаны в " + CSV_FILE);
    }
}
