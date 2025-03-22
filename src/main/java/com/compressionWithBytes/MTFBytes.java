package com.compressionWithBytes;

import java.io.*;
import java.util.*;

public class MTFBytes {
    public static void encode(InputStream input, OutputStream output) throws IOException {
        List<Byte> alphabet = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            alphabet.add((byte) i);
        }

        int byteRead;
        while ((byteRead = input.read()) != -1) {
            byte b = (byte) byteRead;
            int index = alphabet.indexOf(b);
            output.write(index);
            alphabet.remove(index);
            alphabet.add(0, b);
        }
    }

    public static void decode(InputStream input, OutputStream output) throws IOException {
        List<Byte> alphabet = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            alphabet.add((byte) i);
        }

        int byteRead;
        while ((byteRead = input.read()) != -1) {
            byte b = alphabet.get(byteRead);
            output.write(b);
            alphabet.remove(byteRead);
            alphabet.add(0, b);
        }
    }

    public static void main(String[] args) throws IOException {
        File inputFile = new File("dataset/enwik7");
        File encodedFile = new File("testedT.mtf");
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
