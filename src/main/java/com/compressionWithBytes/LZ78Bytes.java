package com.compressionWithBytes;

import java.io.*;

public class LZ78Bytes {
    private static final int WINDOW_SIZE = 4096;
    private static final int LOOKAHEAD_SIZE = 18;
    private static final int MIN_MATCH_LENGTH = 3;

    public static void compress(InputStream inputStream, OutputStream outputStream) throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(outputStream);
        byte[] window = new byte[WINDOW_SIZE + LOOKAHEAD_SIZE];
        int windowEnd = 0, bytesRead;

        while ((bytesRead = inputStream.read(window, windowEnd, LOOKAHEAD_SIZE)) > 0) {
            int bestMatchLength = 0, bestMatchOffset = 0;
            
            for (int i = Math.max(0, windowEnd - WINDOW_SIZE); i < windowEnd; i++) {
                int matchLength = 0;
                while (matchLength < bytesRead && window[i + matchLength] == window[windowEnd + matchLength]) {
                    matchLength++;
                    if (i + matchLength >= windowEnd) break;
                }
                if (matchLength > bestMatchLength && matchLength >= MIN_MATCH_LENGTH) {
                    bestMatchLength = matchLength;
                    bestMatchOffset = windowEnd - i;
                }
            }
            
            if (bestMatchLength >= MIN_MATCH_LENGTH) {
                dataOutput.writeBoolean(true);
                dataOutput.writeShort(bestMatchOffset);
                dataOutput.writeByte(bestMatchLength);
            } else {
                dataOutput.writeBoolean(false);
                dataOutput.writeByte(window[windowEnd]);
                bestMatchLength = 1;
            }
            
            System.arraycopy(window, windowEnd, window, 0, bestMatchLength);
            windowEnd = bestMatchLength;
        }
    }

    public static void decompress(InputStream inputStream, OutputStream outputStream) throws IOException {
        DataInputStream dataInput = new DataInputStream(inputStream);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        
        while (dataInput.available() > 0) {
            boolean isCompressed = dataInput.readBoolean();
            if (isCompressed) {
                int offset = dataInput.readShort();
                int length = dataInput.readByte();
                int start = buffer.size() - offset;
                for (int i = 0; i < length; i++) {
                    buffer.write(buffer.toByteArray()[start + i]);
                }
            } else {
                buffer.write(dataInput.readByte());
            }
        }
        buffer.writeTo(outputStream);
    }

    public static void main(String[] args) throws IOException {
        File inputFile = new File("test");
        File encodedFile = new File("testT.lzss");
        File decodedFile = new File("decoded");

        try (InputStream inputStream = new FileInputStream(inputFile);
             OutputStream outputStream = new FileOutputStream(encodedFile)) {
            compress(inputStream, outputStream);
        }

        try (InputStream inputStream = new FileInputStream(encodedFile);
             OutputStream outputStream = new FileOutputStream(decodedFile)) {
            decompress(inputStream, outputStream);
        }
    }
}