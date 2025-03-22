package com.compressionWithBytes;

import java.io.*;
import java.util.*;

public class BWTTestr {
    public static void encode(InputStream inputStream, OutputStream outputStream) throws IOException{
        byte[] info = inputStream.readAllBytes();
        Integer[] suffixArray = buildSuffixArray(info);
        printSuffixArray(info, suffixArray);
        
        for (int i = 0; i < info.length; i++){
            int index = (suffixArray[i] + info.length - 1) % (info.length);
            outputStream.write(info[index]);
        }
    }

    public static void decode(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bwt = inputStream.readAllBytes();
        int length = bwt.length;

        // Создаем массив для хранения индексов
        int[] indices = new int[length];

        // Инициализируем массив индексов
        for (int i = 0; i < length; i++) {
            indices[i] = i;
        }

        // Сортируем индексы на основе символов в BWT
        Integer[] sortedIndices = Arrays.stream(indices).boxed().toArray(Integer[]::new);
        Arrays.sort(sortedIndices, (a, b) -> Byte.compare(bwt[a], bwt[b]));

        // Восстанавливаем исходную строку
        int[] next = new int[length];
        for (int i = 0; i < length; i++) {
            next[i] = sortedIndices[i];
        }

        // Находим индекс начала исходной строки
        int startIndex = 0;
        for (int i = 0; i < length; i++) {
            if (bwt[i] == 0) { // Предполагаем, что 0 - это символ конца строки
                startIndex = i;
                break;
            }
        }

        // Восстанавливаем строку
        int current = startIndex;
        for (int i = 0; i < length; i++) {
            outputStream.write(bwt[current]);
            current = next[current];
        }
    }

    public static Integer[] buildSuffixArray(byte[] info){
        int N = info.length;
        Integer[] suffixArray = new Integer[N];

        for (int i = 0; i < N; i++){
            suffixArray[i] = i;
        }

        Arrays.sort(suffixArray, (a, b) -> compareSuffixes(info, a, b));

        return suffixArray;
    }

    public static int compareSuffixes(byte[] info, int a, int b) {
        int N = info.length;
        while (a < N && b < N) {
            if (info[a] != info[b]) return Byte.compare(info[a], info[b]);
            a++;
            b++;
        }
        return Integer.compare(N-a, N-b);
    }

    private static void printSuffixArray(byte[] input, Integer[] suffixArray) {
        System.out.println("\nОтсортированные суффиксы:");
        for (int i = 0; i < suffixArray.length; i++) {
            int index = suffixArray[i];
            String suffix = new String(input, index, input.length - index);
            System.out.printf("%2d: %-10s\n", index, suffix);
        }
    }

    public static void main(String[] args) throws IOException {
        File inputFile = new File("test");
        File encodedFile = new File("tested.bwt");
        File decodedFile = new File("decoded");

        try (InputStream inputStream = new FileInputStream(inputFile);
             OutputStream outputStream = new FileOutputStream(encodedFile)) {
            encode(inputStream, outputStream);
        }

        try (InputStream inputStream = new FileInputStream(encodedFile);
             OutputStream outputStream = new FileOutputStream(decodedFile)) {
            decode(inputStream, outputStream);
        }
    }
}