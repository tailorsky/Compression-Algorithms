package com.compressionWithBytes;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;

public class TestAllComps {

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

    public static String compress(String tests, String inputFilePath) throws IOException {
        for (String method : tests.split("\\+")) {
            String outputFilePath = inputFilePath + "." + method.toLowerCase();
            try (InputStream inputStream = new FileInputStream(inputFilePath);
                 OutputStream outputStream = new FileOutputStream(outputFilePath)) {
                COMPRESSORS.getOrDefault(method, (in, out) -> {}).accept(inputStream, outputStream);
            }
            inputFilePath = outputFilePath;
        }
        return inputFilePath;
    }

    public static void decompress(String tests, String outputFilePath) throws IOException {
        List<String> methods = Arrays.asList(tests.split("\\+"));
        Collections.reverse(methods);
        String decomp = "DECOMP";
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
    }

    public static void main(String[] args) throws IOException {
        String inputFilePath = "voina";
        String[] allTests = {"HA", "RLE", "BWT+RLE", "BWT+MTF+HA", "BWT+MTF+RLE+HA", "LZSS", "LZSS+HA", "LZW", "LZW+HA"};
        String tests = "BWT+RLE";
        String outputFilePath = compress(tests, inputFilePath);
        decompress(tests, outputFilePath);
    }
}