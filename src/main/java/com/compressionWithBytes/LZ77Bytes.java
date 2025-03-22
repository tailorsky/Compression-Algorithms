package com.compressionWithBytes;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LZ77Bytes {
    public static final int BUFFER_SIZE = 4096;
    private static final int LOOKAHEAD_BUFFER_SIZE = 18;

    public static void encode(InputStream inputStream, OutputStream outputStream) throws IOException {
        List<Byte> dictionary = new ArrayList<>();
        DataOutputStream dataOut = new DataOutputStream(outputStream);

        int nextByte;
        while ((nextByte = inputStream.read()) != -1) {
            int matchOffset = 0, matchLength = 0;
            byte nextChar = (byte) nextByte;

            for (int i = Math.max(0, dictionary.size() - BUFFER_SIZE); i < dictionary.size(); i++) {
                int j = 0;
                while (j < LOOKAHEAD_BUFFER_SIZE && i + j < dictionary.size() && 
                       dictionary.get(i + j).equals(nextChar)) {
                    j++;
                    if ((nextByte = inputStream.read()) == -1) break;
                    nextChar = (byte) nextByte;
                }
                if (j > matchLength) {
                    matchOffset = dictionary.size() - i;
                    matchLength = j;
                }
            }

            if (matchLength > 1) {
                dataOut.writeBoolean(true);
                dataOut.writeShort(matchOffset);
                dataOut.writeByte(matchLength);
            } else {
                dataOut.writeBoolean(false);
                dataOut.writeByte(nextChar);
            }
            
            for (int k = 0; k < matchLength; k++) {
                dictionary.add(nextChar);
                if (dictionary.size() > BUFFER_SIZE) dictionary.remove(0);
                nextByte = inputStream.read();
                if (nextByte == -1) break;
                nextChar = (byte) nextByte;
            }
        }
    }

    public static void decode(InputStream inputStream, OutputStream outputStream) throws IOException {
        List<Byte> dictionary = new ArrayList<>();
        DataInputStream dataIn = new DataInputStream(inputStream);

        while (dataIn.available() > 0) {
            boolean isReference = dataIn.readBoolean();
            if (isReference) {
                int offset = dataIn.readShort();
                int length = dataIn.readByte();
                int start = dictionary.size() - offset;
                for (int i = 0; i < length; i++) {
                    byte b = dictionary.get(start + i);
                    outputStream.write(b);
                    dictionary.add(b);
                    if (dictionary.size() > BUFFER_SIZE) dictionary.remove(0);
                }
            } else {
                byte nextChar = dataIn.readByte();
                outputStream.write(nextChar);
                dictionary.add(nextChar);
                if (dictionary.size() > BUFFER_SIZE) dictionary.remove(0);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File inputFile = new File("voina");
        File encodedFile = new File("test.lz77");
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
