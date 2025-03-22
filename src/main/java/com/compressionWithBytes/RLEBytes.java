package com.compressionWithBytes;

import java.io.*;
import java.util.*;

public class RLEBytes {
    
    private static final byte MARKER = (byte) 0xFF;

    public static void encode(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        int i = 0;
        while (i < bytes.length) {
            int charLength = 1;
            byte[] currentChar = Arrays.copyOfRange(bytes, i, i + charLength);
            i += charLength;

            int count = 1;
            while (i < bytes.length && Arrays.equals(currentChar, Arrays.copyOfRange(bytes, i, i + charLength))) {
                count++;
                i += charLength;
            }

            if (count > 1 || currentChar[0] == MARKER) {
                outputStream.write(MARKER);
                outputStream.write((count >> 8) & 0xFF);
                outputStream.write(count & 0xFF);
            }
            outputStream.write(currentChar);
        }
    }

    public static void decode(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        int i = 0;
        while (i < bytes.length) {
            if (bytes[i] == MARKER) {
                if (i + 2 >= bytes.length) {
                    throw new IOException("Unexpected end of input while reading count");
                }
                i++;
                int count = ((bytes[i] & 0xFF) << 8) | (bytes[i + 1] & 0xFF);
                i += 2;

                if (i >= bytes.length) {
                    throw new IOException("Unexpected end of input while reading repeated character");
                }

                int charLength = 1;
                byte[] currentChar = Arrays.copyOfRange(bytes, i, i + charLength);
                i += charLength;

                for (int j = 0; j < count; j++) {
                    outputStream.write(currentChar, 0, currentChar.length);
                }
            } else {
                int charLength = 1;
                byte[] currentChar = Arrays.copyOfRange(bytes, i, i + charLength);
                i += charLength;
                outputStream.write(currentChar, 0, currentChar.length);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File inputFile = new File("output.raw");
        File encodedFile = new File("test.rle");
        File decodedFile = new File("tested.raw");

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
