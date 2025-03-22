package com.compressionWithBytes;

import java.io.*;
import java.util.*;

public class LZWBytes {
    private static final int DICT_SIZE = 256;
    
    public static void encode(InputStream inputStream, OutputStream outputStream) throws IOException {
        Map<List<Byte>, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < DICT_SIZE; i++) {
            dictionary.put(Collections.singletonList((byte) i), i);
        }

        List<Byte> currentSequence = new ArrayList<>();
        DataOutputStream dataOutput = new DataOutputStream(outputStream);
        int dictIndex = DICT_SIZE;
        int byteRead;

        while ((byteRead = inputStream.read()) != -1) {
            currentSequence.add((byte) byteRead);
            if (!dictionary.containsKey(currentSequence)) {
                dictionary.put(new ArrayList<>(currentSequence), dictIndex++);
                currentSequence.remove(currentSequence.size() - 1);
                dataOutput.writeInt(dictionary.get(currentSequence));
                currentSequence.clear();
                currentSequence.add((byte) byteRead);
            }
        }

        if (!currentSequence.isEmpty()) {
            dataOutput.writeInt(dictionary.get(currentSequence));
        }
    }

    public static void decode(InputStream inputStream, OutputStream outputStream) throws IOException {
        Map<Integer, List<Byte>> dictionary = new HashMap<>();
        for (int i = 0; i < DICT_SIZE; i++) {
            dictionary.put(i, Collections.singletonList((byte) i));
        }

        DataInputStream dataInput = new DataInputStream(inputStream);
        DataOutputStream dataOutput = new DataOutputStream(outputStream);
        int dictIndex = DICT_SIZE;
        int previousCode = dataInput.readInt();
        List<Byte> currentSequence = new ArrayList<>(dictionary.get(previousCode));
        for (byte b : currentSequence) {
            dataOutput.write(b);
        }
        
        int currentCode;
        while (dataInput.available() > 0) {
            currentCode = dataInput.readInt();
            List<Byte> entry;
            
            if (dictionary.containsKey(currentCode)) {
                entry = new ArrayList<>(dictionary.get(currentCode));
            } else {
                entry = new ArrayList<>(dictionary.get(previousCode));
                entry.add(entry.get(0));
            }
            
            for (byte b : entry) {
                dataOutput.write(b);
            }
            
            List<Byte> newSequence = new ArrayList<>(dictionary.get(previousCode));
            newSequence.add(entry.get(0));
            dictionary.put(dictIndex++, newSequence);
            previousCode = currentCode;
        }
    }

    public static void main(String[] args) throws IOException {
        File inputFile = new File("test");
        File encodedFile = new File("tested.lzw");
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
