package com.compressionWithBytes;

import java.io.*;
import java.util.*;

public class LZSSBytes {
    static int WINDOW_SIZE = 4096;
    private static final int LOOKAHEAD_SIZE = 18;
    private static final int MIN_MATCH_LENGTH = 3;

    public static void encode(InputStream inputStream, OutputStream outputStream) throws IOException {
        List<Byte> window = new ArrayList<>();
        DataOutputStream dataOut = new DataOutputStream(outputStream);
        List<Byte> lookaheadBuffer = new ArrayList<>();
        int nextByte;

        while ((nextByte = inputStream.read()) != -1) {
            lookaheadBuffer.add((byte) nextByte);
            if (lookaheadBuffer.size() >= LOOKAHEAD_SIZE) {
                processBuffer(window, lookaheadBuffer, dataOut);
            }
        }

        while (!lookaheadBuffer.isEmpty()) {
            processBuffer(window, lookaheadBuffer, dataOut);
        }
    }

    public static void processBuffer(List<Byte> window, List<Byte> lookaheadBuffer, DataOutputStream dataOut) throws IOException {
        if (lookaheadBuffer.isEmpty()) return;
        byte[] lookaheadArray = new byte[lookaheadBuffer.size()];
        for (int i = 0; i < lookaheadBuffer.size(); i++) {
            lookaheadArray[i] = lookaheadBuffer.get(i);
        }

        int[] match = findLongestMatch(window, lookaheadArray);
        if (match[1] >= MIN_MATCH_LENGTH) {
            dataOut.write(1);
            dataOut.writeShort(match[0]);
            dataOut.write(match[1]);
            for (int i = 0; i < match[1]; i++) {
                byte b = lookaheadBuffer.remove(0);
                window.add(b);
            }
        } else {
            dataOut.write(0);
            byte b = lookaheadBuffer.remove(0);
            dataOut.write(b);
            window.add(b);
        }

        while (window.size() > WINDOW_SIZE) {
            window.remove(0);
        }
    }

    public static int[] findLongestMatch(List<Byte> window, byte[] lookahead) {
        int bestMatchDistance = 0;
        int bestMatchLength = 0;
        for (int i = 0; i < window.size(); i++) {
            int length = 0;
            while (length < lookahead.length && i + length < window.size() && window.get(i + length) == lookahead[length]) {
                length++;
            }
            if (length > bestMatchLength) {
                bestMatchDistance = window.size() - i;
                bestMatchLength = length;
            }
        }
        return new int[]{bestMatchDistance, bestMatchLength};
    }

    public static void decode(InputStream inputStream, OutputStream outputStream) throws IOException {
        List<Byte> window = new ArrayList<>();
        DataInputStream dataIn = new DataInputStream(inputStream);
        int flag;
        while (dataIn.available() > 0) {
            flag = dataIn.read();
            if (flag == 1) {
                int offset = dataIn.readShort();
                int length = dataIn.read();
                for (int i = 0; i < length; i++) {
                    byte b = window.get(window.size() - offset);
                    outputStream.write(b);
                    window.add(b);
                }
            } else {
                byte b = dataIn.readByte();
                outputStream.write(b);
                window.add(b);
            }
            while (window.size() > WINDOW_SIZE) {
                window.remove(0);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File inputFile = new File("BW.raw");
        File encodedFile = new File("tested");
        File decodedFile = new File("decoded.raw");

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