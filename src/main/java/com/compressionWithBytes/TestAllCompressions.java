package com.compressionWithBytes;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;

public class TestAllCompressions {

    private static final Map<String, BiConsumer<InputStream, OutputStream>> COMPRESSORS = Map.of(
        "HA", wrap(HuffmanBytes::encode),
        "BWT", wrap(BWTBytes::encode),
        "RLE", wrap(RLEBytes::encode),
        "LZSS", wrap(LZSSBytes::encode),
        "LZW", wrap(LZWBytes::encode),
        "MTF", wrap(MTFBytes::encode)
    );

    private static final Map<String, BiConsumer<InputStream, OutputStream>> DECOMPRESSORS = Map.of(
        "BWT", wrap(BWTBytes::decode),
        "RLE", wrap(RLEBytes::decode),
        "LZSS", wrap(LZSSBytes::decode),
        "LZW", wrap(LZWBytes::decode),
        "MTF", wrap(MTFBytes::decode)
    );

    private static final String CSV_FILE = "compression_results.csv";

    static {
        try (FileWriter writer = new FileWriter(CSV_FILE)) {
            writer.write("Test,Original Size,Compressed Size,Compression coefficient,Compression Time,Decompression Time,Total Time\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BiConsumer<InputStream, OutputStream> wrap(BiConsumerWithIOException<InputStream, OutputStream> consumer) {
        return (in, out) -> {
            try {
                consumer.accept(in, out);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    @FunctionalInterface
    interface BiConsumerWithIOException<T, U> {
        void accept(T t, U u) throws IOException;
    }

    public static long compress(String tests, String inputFilePath) throws IOException {
        long startTime = System.nanoTime();
        long originalSize = new File(inputFilePath).length();

        for (String method : tests.split("\\+")) {
            String outputFilePath = inputFilePath + "." + method.toLowerCase();
            try (InputStream inputStream = new FileInputStream(inputFilePath);
                 OutputStream outputStream = new FileOutputStream(outputFilePath)) {
                COMPRESSORS.getOrDefault(method, (in, out) -> {}).accept(inputStream, outputStream);
            }
            inputFilePath = outputFilePath;
        }

        long compressedSize = new File(inputFilePath).length();
        long endTime = System.nanoTime();
        long compressionTime = (endTime - startTime) / 1_000_000;

        return compressionTime;
    }

    public static long decompress(String tests, String outputFilePath) throws IOException {
        List<String> methods = Arrays.asList(tests.split("\\+"));
        Collections.reverse(methods);
        long startTime = System.nanoTime();

        String decomp = "_DECOMP";
        for (String method : methods) {
            String inputFilePath = outputFilePath;
            outputFilePath = outputFilePath.replaceAll("\\." + method.toLowerCase() + "$", "");
            decomp = decomp + "_" + method;
            if (method.equals(methods.get(methods.size() - 1))) {
                int dotIndex = outputFilePath.lastIndexOf(".");
                if (dotIndex > 0)
                    outputFilePath = outputFilePath.substring(0, dotIndex) + decomp + outputFilePath.substring(dotIndex);
                else outputFilePath = outputFilePath + decomp;
            }
            if ("HA".equals(method)){
                CodeTreeNode tree = HuffmanBytes.getTree(outputFilePath.replaceAll(decomp, ""));
                try (InputStream inputStream = new FileInputStream(inputFilePath);
                    OutputStream outputStream = new FileOutputStream(outputFilePath)) {
                    HuffmanBytes.decode(inputStream, outputStream, tree);
                }
            }
            else{
                    try (InputStream inputStream = new FileInputStream(inputFilePath);
                    OutputStream outputStream = new FileOutputStream(outputFilePath)) {
                    DECOMPRESSORS.getOrDefault(method, (in, out) -> {}).accept(inputStream, outputStream);
                }
            }
        }

        long endTime = System.nanoTime();
        long decompressionTime = (endTime - startTime) / 1_000_000;
        return decompressionTime;
    }

    public static void logResults(String test, long originalSize, long compressedSize, long compressionTime, long decompressionTime, long totalTime) {
        boolean fileExists = new File(CSV_FILE).exists();
        try (FileWriter writer = new FileWriter(CSV_FILE, true)) {
            if (!fileExists) {
                writer.write("Test,Original Size,Compressed Size, Compression coefficient,Compression Time,Decompression Time,Total Time\n");
            }
            float CompressionCoefficient = (float)originalSize/(float)compressedSize;
            String resultLine = String.format(Locale.US,"%s,%d,%d,%.3f,%d,%d,%d\n", test, originalSize, compressedSize, CompressionCoefficient, compressionTime, decompressionTime, totalTime);
            writer.write(resultLine);
            System.out.printf("Test: %s | Original Size: %d | Compressed Size: %d | Compression Coefficient: %.3f | Compression Time: %dms | Decompression Time: %dms | Total Time: %dms\n", 
                              test, originalSize, compressedSize, CompressionCoefficient, compressionTime, decompressionTime, totalTime);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getOutputFilePath(String inputFilePath, String test){
        String outputFilePath = "";
        
        for (String method : test.split("\\+")) {
            outputFilePath = inputFilePath + "." + method.toLowerCase();
            inputFilePath = outputFilePath;
        }
        return outputFilePath;
    }
    public static void main(String[] args) throws IOException {
        String inputFilePath = "bw_image/bw.raw";
        String[] allTests = {"HA", "RLE", "BWT+RLE", "BWT+MTF+HA", "BWT+MTF+RLE+HA", "LZSS", "LZSS+HA", "LZW", "LZW+HA"};
        for (String tests : allTests) {
            long totalStart = System.nanoTime();
            long compressionTime = compress(tests, inputFilePath);
            String outputFilePath = getOutputFilePath(inputFilePath, tests);
            long decompressionTime = decompress(tests, outputFilePath);
            long totalEnd = System.nanoTime();
            long totalTime = (totalEnd - totalStart) / 1_000_000;
            logResults(tests, new File(inputFilePath).length(), new File(outputFilePath).length(), compressionTime, decompressionTime, totalTime);
        }
    }
}
